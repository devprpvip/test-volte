package com.example.data.band

import android.content.Context
import android.os.Build
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Kiểm tra band (LTE/NR) + quốc gia + máy, đối chiếu với CarrierBandDatabase.
 * Dựa trên docs Android:
 * - CellIdentityLte.getEarfcn() (API 24+) → map sang E-UTRA band qua 3GPP 36.101
 * - CellIdentityNr.getNrarfcn() (API 29+) → map NR band
 * - ServiceState.getChannelNumber() (API 34+) = EARFCN/NRARFCN trực tiếp
 * - PhysicalChannelConfig.getBand() (API 31+) là band đã phân giải
 * - TelephonyManager.getNetworkCountryIso() + SubscriptionInfo.countryIso
 * - Build.MANUFACTURER/MODEL để nhận diện hãng
 *
 * Tài liệu band VN 2024-2026: Viettel/Vina/Mobi dùng B3 (1800 MHz FDD) chính,
 * thêm B1 (2100), B8 (900), 5G n78 (3500), n41 (2500) – spectrum-tracker.com
 */
class BandCheckManager(private val context: Context) {

    private val telephonyManager: TelephonyManager? by lazy {
        try { context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager } catch (_: Throwable) { null }
    }
    private val subscriptionManager: SubscriptionManager? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager else null
        } catch (_: Throwable) { null }
    }

    data class BandInfo(
        val earfcn: Int? = null,
        val nrarfcn: Int? = null,
        val channelNumber: Int? = null,
        val band: Int? = null, // E-UTRA band number, ví dụ 3 = B3
        val bandLabel: String, // "B3 (1800 MHz)"
        val ratType: String, // "LTE", "NR", "WCDMA", "GSM"
        val isDetected: Boolean
    )

    data class CountryInfo(
        val countryIso: String, // "vn", "us", "jp"
        val countryName: String, // "Việt Nam"
        val mcc: String? = null, // "452"
        val operatorName: String? = null
    )

    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val brand: String,
        val androidVersion: String
    )

    data class BandCheckResult(
        val device: DeviceInfo,
        val country: CountryInfo,
        val band: BandInfo,
        val carrierMatch: CarrierBandDatabase.CarrierBandInfo?,
        val compatible: Boolean,
        val supportLevel: SupportLevel,
        val fixGuide: FixGuide?
    )

    enum class SupportLevel { FULL, PARTIAL, UNKNOWN, NO_BAND }

    data class FixGuide(
        val title: String,
        val summary: String,
        val steps: List<String>,
        val secretCode: String?,
        val shizukuScriptId: String?,
        val warning: String? = null
    )

    fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        manufacturer = Build.MANUFACTURER ?: "Unknown",
        model = Build.MODEL ?: "Unknown",
        brand = Build.BRAND ?: Build.MANUFACTURER ?: "Unknown",
        androidVersion = "Android ${Build.VERSION.RELEASE ?: ""} (API ${Build.VERSION.SDK_INT})"
    )

    fun getCountryInfo(): CountryInfo {
        var iso = ""
        var mcc: String? = null
        var operator: String? = null
        try {
            iso = telephonyManager?.networkCountryIso ?: ""
            if (iso.isBlank()) iso = telephonyManager?.simCountryIso ?: ""
            // Thử lấy từ SubscriptionManager
            if (iso.isBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subs = try { subscriptionManager?.activeSubscriptionInfoList } catch (_: Throwable) { null }
                iso = subs?.firstOrNull()?.countryIso ?: ""
                mcc = subs?.firstOrNull()?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.mccString else @Suppress("DEPRECATION") it.mcc.toString()
                }
            }
            mcc = mcc ?: try { telephonyManager?.networkOperator?.takeIf { it.length >= 3 }?.substring(0, 3) } catch (_: Throwable) { null }
            operator = try { telephonyManager?.networkOperatorName ?: telephonyManager?.simOperatorName } catch (_: Throwable) { null }
        } catch (_: Throwable) {}
        if (iso.isBlank()) iso = "vn" // fallback VN vì app tập trung VN
        val name = CarrierBandDatabase.countryNameForIso(iso)
        return CountryInfo(countryIso = iso.lowercase(), countryName = name, mcc = mcc, operatorName = operator)
    }

    /**
     * Lấy band hiện tại từ CellInfo / ServiceState / PhysicalChannelConfig.
     * Ưu tiên: CellInfoLte.getEarfcn() → map band → PhysicalChannelConfig → ServiceState.channelNumber
     */
    fun getCurrentBand(): BandInfo {
        // Thử CellInfo trước (cần ACCESS_FINE_LOCATION hoặc READ_PHONE_STATE + location)
        try {
            val allCellInfo = try { telephonyManager?.allCellInfo } catch (se: SecurityException) { null } catch (_: Throwable) { null }
            if (!allCellInfo.isNullOrEmpty()) {
                // Tìm cell đã registered
                val registered = allCellInfo.firstOrNull { it.isRegistered } ?: allCellInfo.firstOrNull()
                registered?.let { ci ->
                    when (ci) {
                        is CellInfoLte -> {
                            val identity = ci.cellIdentity
                            val earfcn = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) identity.earfcn else -1
                            } catch (_: Throwable) { -1 }
                            if (earfcn != -1 && earfcn != Int.MAX_VALUE) {
                                val band = earfcnToBand(earfcn)
                                return BandInfo(
                                    earfcn = earfcn,
                                    band = band,
                                    bandLabel = band?.let { "B$it (${eutranBandFrequency(it)})" } ?: "EARFCN $earfcn (band chưa map)",
                                    ratType = "LTE",
                                    isDetected = band != null
                                )
                            }
                            // Fallback: thử getBand() via reflection (API 30+ hidden band)
                            val bandViaReflection = try {
                                val m = identity.javaClass.getMethod("getBand")
                                m.invoke(identity) as? Int
                            } catch (_: Throwable) { null }
                            if (bandViaReflection != null && bandViaReflection != 0) {
                                return BandInfo(band = bandViaReflection, bandLabel = "B$bandViaReflection (${eutranBandFrequency(bandViaReflection)})", ratType = "LTE", isDetected = true)
                            }
                        }
                        is CellInfoWcdma -> {
                            return BandInfo(bandLabel = "WCDMA (3G)", ratType = "WCDMA", isDetected = false)
                        }
                        is CellInfoGsm -> {
                            return BandInfo(bandLabel = "GSM (2G)", ratType = "GSM", isDetected = false)
                        }
                        else -> {
                            // Android 10+ có CellInfoNr
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                try {
                                    if (ci.javaClass.simpleName == "CellInfoNr") {
                                        val getIdentity = ci.javaClass.getMethod("getCellIdentity")
                                        val identity = getIdentity.invoke(ci)
                                        val getNrarfcn = identity.javaClass.getMethod("getNrarfcn")
                                        val nrarfcn = getNrarfcn.invoke(identity) as? Int
                                        if (nrarfcn != null && nrarfcn != -1) {
                                            val band = nrarfcnToBand(nrarfcn)
                                            return BandInfo(nrarfcn = nrarfcn, band = band, bandLabel = band?.let { "n$it (${nranBandFrequency(it)})" } ?: "NRARFCN $nrarfcn", ratType = "NR", isDetected = band != null)
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        // Thử ServiceState.getChannelNumber() (API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val serviceState = try { telephonyManager?.serviceState } catch (_: Throwable) { null }
                val ch = try { serviceState?.channelNumber } catch (_: Throwable) { -1 }
                if (ch != null && ch != -1 && ch != Int.MAX_VALUE) {
                    // Thử đoán band qua EARFCN table
                    val band = earfcnToBand(ch) ?: nrarfcnToBand(ch)
                    val rat = try {
                        val m = serviceState?.javaClass?.getMethod("getRilDataRadioTechnology")
                        m?.invoke(serviceState) as? Int
                    } catch (_: Throwable) { null }
                    val ratStr = when (rat) {
                        TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                        TelephonyManager.NETWORK_TYPE_NR -> "NR"
                        else -> "LTE/NR"
                    }
                    return BandInfo(channelNumber = ch, band = band, bandLabel = band?.let { "B$it/${ch}" } ?: "Channel $ch", ratType = ratStr, isDetected = band != null)
                }
            } catch (_: Throwable) {}
        }

        // Thử PhysicalChannelConfig (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // TelephonyManager.getPhysicalChannelConfig is hidden but có thể gọi qua reflection
                val tm = telephonyManager ?: return fallbackBand()
                val m = try { tm.javaClass.getMethod("getPhysicalChannelConfig") } catch (_: Throwable) { null }
                val configs = try { m?.invoke(tm) as? List<*> } catch (_: Throwable) { null }
                val first = configs?.firstOrNull()
                if (first != null) {
                    val getBand = try { first.javaClass.getMethod("getBand") } catch (_: Throwable) { null }
                    val band = try { getBand?.invoke(first) as? Int } catch (_: Throwable) { null }
                    if (band != null && band != 0) {
                        return BandInfo(band = band, bandLabel = "B$band (${eutranBandFrequency(band)})", ratType = "LTE", isDetected = true)
                    }
                }
            } catch (_: Throwable) {}
        }

        return fallbackBand()
    }

    private fun fallbackBand(): BandInfo {
        // Không đọc được band thực → giả lập dựa trên networkOperator + quốc gia VN → B3
        val country = getCountryInfo()
        val carrier = telephonyManager?.networkOperatorName ?: ""
        // VN mặc định B3
        val isVn = country.countryIso == "vn" || country.mcc == "452"
        return if (isVn) {
            BandInfo(band = 3, bandLabel = "B3 (1800 MHz - giả định VN, cần quyền Vị trí để đọc thực)", ratType = "LTE (giả định)", isDetected = false)
        } else {
            BandInfo(bandLabel = "Chưa xác định (cần cấp quyền Vị trí + Điện thoại)", ratType = "Unknown", isDetected = false)
        }
    }

    /**
     * EARFCN → E-UTRA band (3GPP 36.101 Table 5.7.3-1, rút gọn các band phổ biến VN/quốc tế)
     * Tham khảo: http://niviuk.free.fr/lte_band.php
     */
    fun earfcnToBand(earfcn: Int): Int? = when (earfcn) {
        in 0..599 -> 1        // B1 2100 FDD
        in 600..1199 -> 2     // B2 1900
        in 1200..1949 -> 3    // B3 1800
        in 1950..2399 -> 4
        in 2400..2649 -> 5
        in 2650..2749 -> 6
        in 2750..3449 -> 7    // B7 2600
        in 3450..3799 -> 8    // B8 900
        in 3800..4149 -> 9
        in 4150..4749 -> 10
        in 4750..4999 -> 11
        in 5000..5179 -> 12
        in 5180..5279 -> 13
        in 5280..5379 -> 14
        in 5380..5489 -> 17
        in 5490..5599 -> 18
        in 5600..5749 -> 19
        in 5750..5999 -> 20   // B20 800
        in 6000..6149 -> 21
        in 6150..6449 -> 22
        // TDD 33-41
        in 36000..36199 -> 33
        in 36200..36349 -> 34
        in 36350..36949 -> 35
        in 36950..37549 -> 36
        in 37550..37749 -> 37
        in 37750..38249 -> 38
        in 38250..38649 -> 39
        in 38650..39649 -> 40 // B40 2300 TDD (VN)
        in 39650..41589 -> 41 // B41 2500 TDD
        else -> null
    }

    fun nrarfcnToBand(nrarfcn: Int): Int? = when (nrarfcn) {
        // FR1 n78 3500, n41 2500, n1, n3, n7, n8, n28
        in 620000..680000 -> 78 // n78
        in 499200..537999 -> 41 // n41
        in 422000..434000 -> 1
        in 386000..399000 -> 3
        in 620000..653333 -> 77 // n77
        else -> null
    }

    fun eutranBandFrequency(band: Int): String = when (band) {
        1 -> "2100 MHz FDD"
        2 -> "1900 MHz FDD"
        3 -> "1800 MHz FDD"
        7 -> "2600 MHz FDD"
        8 -> "900 MHz FDD"
        20 -> "800 MHz FDD"
        28 -> "700 MHz FDD"
        38 -> "2600 MHz TDD"
        40 -> "2300 MHz TDD"
        41 -> "2500 MHz TDD"
        else -> "${band}"
    }

    fun nranBandFrequency(band: Int): String = when (band) {
        1 -> "2100 MHz"
        3 -> "1800 MHz"
        7 -> "2600 MHz"
        8 -> "900 MHz"
        28 -> "700 MHz"
        41 -> "2500 MHz"
        77 -> "3700 MHz"
        78 -> "3500 MHz"
        else -> "n$band"
    }

    fun check(): BandCheckResult {
        val device = getDeviceInfo()
        val country = getCountryInfo()
        val band = getCurrentBand()
        val carrierMatch = CarrierBandDatabase.findCarrier(country.countryIso, country.operatorName, country.mcc)
        val bandsForCountry = CarrierBandDatabase.bandsForCountry(country.countryIso)
        val compatible: Boolean
        val level: SupportLevel
        if (band.band == null || !band.isDetected) {
            compatible = bandsForCountry.any { it.band in listOf(1, 3, 7, 8, 20, 40) } // VN hỗ trợ chung
            level = SupportLevel.UNKNOWN
        } else {
            compatible = carrierMatch?.bands?.any { it.band == band.band } ?: (band.band in bandsForCountry.map { it.band })
            level = if (compatible) SupportLevel.FULL else SupportLevel.PARTIAL
        }
        val fix = getFixGuide(device, country, band, carrierMatch)
        return BandCheckResult(device, country, band, carrierMatch, compatible, level, fix)
    }

    fun getFixGuide(device: DeviceInfo, country: CountryInfo, band: BandInfo, carrier: CarrierBandDatabase.CarrierBandInfo?): FixGuide {
        val brandLower = device.brand.lowercase()
        val manufacturerLower = device.manufacturer.lowercase()
        val isVn = country.countryIso == "vn"
        // Ưu tiên theo hãng - tài liệu tổng hợp từ BrandGuides + Shizuku scripts
        return when {
            brandLower.contains("xiaomi") || brandLower.contains("redmi") || brandLower.contains("poco") || manufacturerLower.contains("xiaomi") ->
                FixGuide(
                    title = "Xiaomi / Redmi / POCO – Hiện VoLTE & khóa band",
                    summary = if (isVn) "MIUI/HyperOS ẩn VoLTE do carrier check. Dùng *#*#86583#*#* + kiểm tra band B3." else "Bật VoLTE qua mã ẩn hoặc Shizuku nếu band bị khóa.",
                    steps = listOf(
                        "1. Gõ *#*#86583#*#* trên bàn phím → hiện 'VoLTE carrier check disabled'.",
                        "2. Gõ *#*#869434#*#* nếu cần VoWiFi.",
                        "3. Cài đặt → SIM & Mạng di động → Bật 'Cuộc gọi VoLTE'.",
                        "4. Kiểm tra band: hiện tại ${band.bandLabel}. VN cần B3 (1800) / B1 (2100) / B8 (900). Nếu đang ở B40 TDD, thử *#*#3646633#*#* → BandMode → chọn B3.",
                        "5. Nếu vẫn không hiện, dùng Shizuku script 'Hiện công tắc Enhanced 4G LTE' trong tab Shizuku."
                    ),
                    secretCode = "*#*#86583#*#*",
                    shizukuScriptId = "cc_enhanced_4g_visible"
                )
            brandLower.contains("google") || manufacturerLower.contains("google") ->
                FixGuide(
                    title = "Google Pixel – Mở khóa VoLTE theo vùng",
                    summary = "Pixel khóa VoLTE theo quốc gia. Tại ${country.countryName} (${country.countryIso.uppercase()}), cần override CarrierConfig qua Shizuku.",
                    steps = listOf(
                        "1. Cài Shizuku, bật Wireless Debugging, cấp quyền 'Allow all the time'.",
                        "2. Trong app này → Shizuku → 'Kích hoạt toàn diện Pixel (VoLTE+VoWiFi+VoNR)'.",
                        "3. Hoặc thủ công: cmd phone cc set-value -p carrier_volte_available_bool true (band ${band.bandLabel} sẽ được ép hỗ trợ).",
                        "4. Khởi động lại 2-3 lần, kiểm tra *#*#4636#*#* → IMS Registration = Registered.",
                        "5. Nếu Android 16 QPR2+, persistent bị chặn → dùng 'IMS provisioning' trong Shizuku (không mất sau reboot)."
                    ),
                    secretCode = "*#*#4636#*#*",
                    shizukuScriptId = "pixel_full_enable_all",
                    warning = "Pixel IMS cần Shizuku Ready (ADB uid 2000 hoặc Root)."
                )
            brandLower.contains("samsung") ->
                FixGuide(
                    title = "Samsung – Bật VoLTE & kiểm tra CSC",
                    summary = "Samsung thường tự bật VoLTE; nếu không thấy do CSC khác vùng.",
                    steps = listOf(
                        "1. Cài đặt → Kết nối → Các mạng di động → Bật 'Cuộc gọi VoLTE' cho từng SIM.",
                        "2. Nếu không thấy: gõ *#0011# kiểm tra IMS Registered; *#2263# → chọn Band Selection → đảm bảo B3/B1/B8 được chọn.",
                        "3. Đổi CSC (nếu máy xách tay): cần Odin hoặc Shizuku script 'Hiện IMS Registration Status'.",
                        "4. Band hiện tại ${band.bandLabel} → VN cần B3/B1; nếu đang ở band lạ, thử chọn lại trong *#2263#.",
                    ),
                    secretCode = "*#0011#",
                    shizukuScriptId = "cc_ims_registration_status"
                )
            brandLower.contains("oppo") || brandLower.contains("realme") || brandLower.contains("oneplus") || brandLower.contains("vivo") ->
                FixGuide(
                    title = "Oppo / Realme / OnePlus / Vivo – Engineer Mode",
                    summary = "ColorOS/OxygenOS ẩn VoLTE trong cài đặt nâng cao SIM; có thể mở qua Engineer Mode.",
                    steps = listOf(
                        "1. Cài đặt → Mạng di động → chọn SIM → bật 'Cuộc gọi VoLTE'.",
                        "2. Nếu không thấy: gõ *#800# (Oppo/OnePlus) hoặc *#*#4636#*#* → Phone Information → bật 'VoLTE Provisioned'.",
                        "3. Kiểm tra band ${band.bandLabel}: VN cần B3; vào Engineer Mode → Telephony → BandMode → bật tất cả FDD bands.",
                        "4. Nếu vẫn ẩn, dùng Shizuku 'Bật IMS provisioning' cho sub tương ứng."
                    ),
                    secretCode = "*#800#",
                    shizukuScriptId = "ims_provision_volte"
                )
            brandLower.contains("sony") || brandLower.contains("asus") || brandLower.contains("nokia") ->
                FixGuide(
                    title = "Sony / Asus / Nokia – Testing Menu",
                    summary = "Android thuần: VoLTE trong Radio Info.",
                    steps = listOf(
                        "1. Gõ *#*#4636#*#* → Phone Information → bật 'VoLTE Provisioned' ON.",
                        "2. Nhấn 3 chấm → IMS Service Status → kiểm tra Registered.",
                        "3. Band ${band.bandLabel} → nếu khác B3/B1/B8, thử đổi trong *#*#4636#*#* → Set Preferred Network Type = LTE only rồi lại LTE/WCDMA/GSM.",
                    ),
                    secretCode = "*#*#4636#*#*",
                    shizukuScriptId = null
                )
            else ->
                FixGuide(
                    title = "Máy ${device.manufacturer} ${device.model} – Hướng chung",
                    summary = "Áp dụng cho tất cả hãng chưa liệt kê; tại ${country.countryName} band yêu cầu ${CarrierBandDatabase.bandsForCountry(country.countryIso).joinToString { "B${it.band}" }}.",
                    steps = buildList {
                        add("1. Thử mã chung *#*#4636#*#* → kiểm tra 'VoLTE Provisioned' đã bật chưa.")
                        add("2. Band hiện tại: ${band.bandLabel}. Quốc gia ${country.countryName} hỗ trợ: ${CarrierBandDatabase.bandsForCountry(country.countryIso).joinToString { "B${it.band} (${it.frequency})" }}.")
                        if (carrier != null) {
                            add("3. Nhà mạng ${carrier.carrierName} hỗ trợ: ${carrier.bands.joinToString { "B${it.band}" }} → nếu band hiện tại không trong danh sách, có thể do chưa bật VoLTE hoặc chưa chọn band phù hợp.")
                        }
                        add("4. Vào Cài đặt → Mạng di động → Bật VoLTE / Cuộc gọi 4G.")
                        add("5. Nếu nút bị ẩn, thử các mã: Xiaomi *#*#86583#*#*, Samsung *#0011#, MediaTek *#*#3646633#*#*.")
                        add("6. Cuối cùng, dùng Shizuku script 'Kích hoạt toàn diện' để ép carrier config + IMS provisioning.")
                    },
                    secretCode = "*#*#4636#*#*",
                    shizukuScriptId = "pixel_full_enable_all"
                )
        }
    }
}
