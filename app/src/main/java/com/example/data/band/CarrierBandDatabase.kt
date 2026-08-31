package com.example.data.band

/**
 * Database tĩnh về band LTE/5G theo quốc gia & nhà mạng.
 * Nguồn: spectrum-tracker.com (2024), kimovil.com, gsmarena.com, frequencycheck.com
 * Cập nhật VN 2024-2025: Viettel/Vina/Mobi dùng B3 (1800) chính, B1 (2100), B8 (900), 5G n78/n41.
 * Các nước khác: tổng hợp band phổ biến để app hiển thị compatible hay không.
 */
object CarrierBandDatabase {

    data class BandSpec(val band: Int, val frequency: String, val type: String = "FDD", val note: String? = null)
    data class CarrierBandInfo(
        val countryIso: String,
        val countryName: String,
        val mcc: String?,
        val carrierKey: String,
        val carrierName: String,
        val bands: List<BandSpec>,
        val volteBands: List<Int> // band hỗ trợ VoLTE
    )

    private val ALL_CARRIERS = listOf(
        // ===== Vietnam (MCC 452) =====
        CarrierBandInfo(
            countryIso = "vn", countryName = "Việt Nam", mcc = "452",
            carrierKey = "viettel", carrierName = "Viettel Telecom",
            bands = listOf(
                BandSpec(3, "1800 MHz FDD", "FDD", "Băng chính, phủ toàn quốc"),
                BandSpec(1, "2100 MHz FDD", "FDD", "Bổ sung thành phố"),
                BandSpec(8, "900 MHz FDD", "FDD", "Nông thôn, trong nhà"),
                BandSpec(40, "2300 MHz TDD", "TDD", "TDD bổ sung"),
                BandSpec(78, "3500 MHz", "TDD", "5G n78"),
                BandSpec(41, "2500 MHz", "TDD", "5G n41")
            ), volteBands = listOf(3, 1, 8, 40)
        ),
        CarrierBandInfo(
            countryIso = "vn", countryName = "Việt Nam", mcc = "452",
            carrierKey = "vinaphone", carrierName = "VinaPhone (VNPT)",
            bands = listOf(
                BandSpec(3, "1800 MHz FDD", "FDD", "Băng chính duy nhất theo frequencycheck"),
                BandSpec(8, "900 MHz FDD", "FDD", "Được cấp tần số 900"),
                BandSpec(40, "2300 MHz TDD", "TDD"),
                BandSpec(78, "3500 MHz", "TDD", "5G")
            ), volteBands = listOf(3, 8)
        ),
        CarrierBandInfo(
            countryIso = "vn", countryName = "Việt Nam", mcc = "452",
            carrierKey = "mobifone", carrierName = "MobiFone",
            bands = listOf(
                BandSpec(3, "1800 MHz FDD", "FDD", "Băng chính"),
                BandSpec(8, "900 MHz FDD", "FDD"),
                BandSpec(40, "2300 MHz TDD", "TDD"),
                BandSpec(78, "3500 MHz", "TDD", "5G")
            ), volteBands = listOf(3, 8)
        ),
        CarrierBandInfo(
            countryIso = "vn", countryName = "Việt Nam", mcc = "452",
            carrierKey = "vietnamobile", carrierName = "Vietnamobile",
            bands = listOf(BandSpec(8, "900 MHz FDD", "FDD"), BandSpec(3, "1800 MHz FDD", "FDD")),
            volteBands = listOf(8, 3)
        ),
        CarrierBandInfo(
            countryIso = "vn", countryName = "Việt Nam", mcc = "452",
            carrierKey = "wintel", carrierName = "Wintel (hạ tầng Vina)",
            bands = listOf(BandSpec(3, "1800 MHz FDD", "FDD"), BandSpec(8, "900 MHz FDD", "FDD")),
            volteBands = listOf(3)
        ),
        // ===== United States (MCC 310-316) =====
        CarrierBandInfo(
            countryIso = "us", countryName = "Hoa Kỳ", mcc = "310",
            carrierKey = "tmobile_us", carrierName = "T-Mobile US",
            bands = listOf(BandSpec(2, "1900 MHz"), BandSpec(4, "1700/2100"), BandSpec(12, "700"), BandSpec(41, "2500 TDD"), BandSpec(71, "600")),
            volteBands = listOf(2, 4, 12, 71)
        ),
        CarrierBandInfo(
            countryIso = "us", countryName = "Hoa Kỳ", mcc = "310",
            carrierKey = "att", carrierName = "AT&T",
            bands = listOf(BandSpec(2, "1900"), BandSpec(4, "1700"), BandSpec(12, "700"), BandSpec(17, "700"), BandSpec(40, "2300")),
            volteBands = listOf(2, 4, 12)
        ),
        // ===== Japan (MCC 440-441) =====
        CarrierBandInfo(
            countryIso = "jp", countryName = "Nhật Bản", mcc = "440",
            carrierKey = "docomo", carrierName = "NTT Docomo",
            bands = listOf(BandSpec(1, "2100"), BandSpec(3, "1800"), BandSpec(19, "800"), BandSpec(21, "1500")),
            volteBands = listOf(1, 3, 19)
        ),
        // ===== Korea (MCC 450) =====
        CarrierBandInfo(
            countryIso = "kr", countryName = "Hàn Quốc", mcc = "450",
            carrierKey = "skt", carrierName = "SK Telecom",
            bands = listOf(BandSpec(1, "2100"), BandSpec(3, "1800"), BandSpec(5, "850"), BandSpec(7, "2600")),
            volteBands = listOf(3, 7)
        ),
        // ===== Germany (MCC 262) =====
        CarrierBandInfo(
            countryIso = "de", countryName = "Đức", mcc = "262",
            carrierKey = "telekom_de", carrierName = "Telekom DE",
            bands = listOf(BandSpec(3, "1800"), BandSpec(7, "2600"), BandSpec(20, "800"), BandSpec(28, "700")),
            volteBands = listOf(3, 7, 20)
        ),
        // ===== India (MCC 404-405) =====
        CarrierBandInfo(
            countryIso = "in", countryName = "Ấn Độ", mcc = "404",
            carrierKey = "jio", carrierName = "Jio",
            bands = listOf(BandSpec(3, "1800"), BandSpec(5, "850"), BandSpec(40, "2300 TDD")),
            volteBands = listOf(3, 5, 40)
        ),
        // ===== UK (MCC 234) =====
        CarrierBandInfo(
            countryIso = "gb", countryName = "Vương quốc Anh", mcc = "234",
            carrierKey = "ee", carrierName = "EE",
            bands = listOf(BandSpec(3, "1800"), BandSpec(7, "2600"), BandSpec(20, "800")),
            volteBands = listOf(3, 7, 20)
        ),
        // ===== Thailand (MCC 520) =====
        CarrierBandInfo(
            countryIso = "th", countryName = "Thái Lan", mcc = "520",
            carrierKey = "ais", carrierName = "AIS",
            bands = listOf(BandSpec(1, "2100"), BandSpec(3, "1800"), BandSpec(8, "900")),
            volteBands = listOf(3, 1)
        ),
        // ===== Default global =====
    )

    fun findCarrier(countryIso: String, operatorName: String?, mcc: String?): CarrierBandInfo? {
        val iso = countryIso.lowercase()
        val opLower = operatorName?.lowercase() ?: ""
        // Ưu tiên match theo tên
        return ALL_CARRIERS.filter { it.countryIso == iso }.firstOrNull { c ->
            opLower.contains(c.carrierKey) || opLower.contains(c.carrierName.lowercase().substringBefore(" ")) || (c.mcc != null && c.mcc == mcc)
        } ?: ALL_CARRIERS.firstOrNull { it.countryIso == iso }
            ?: if (mcc == "452") ALL_CARRIERS.firstOrNull { it.countryIso == "vn" } else null
    }

    fun bandsForCountry(countryIso: String): List<BandSpec> {
        val iso = countryIso.lowercase()
        val carriers = ALL_CARRIERS.filter { it.countryIso == iso }
        if (carriers.isNotEmpty()) return carriers.flatMap { it.bands }.distinctBy { it.band }
        // Fallback global phổ biến
        return listOf(BandSpec(1, "2100"), BandSpec(3, "1800"), BandSpec(7, "2600"), BandSpec(8, "900"), BandSpec(20, "800"))
    }

    fun countryNameForIso(iso: String): String = when (iso.lowercase()) {
        "vn" -> "Việt Nam"
        "us" -> "Hoa Kỳ"
        "jp" -> "Nhật Bản"
        "kr" -> "Hàn Quốc"
        "de" -> "Đức"
        "in" -> "Ấn Độ"
        "gb" -> "Vương quốc Anh"
        "th" -> "Thái Lan"
        "cn" -> "Trung Quốc"
        "fr" -> "Pháp"
        "sg" -> "Singapore"
        "my" -> "Malaysia"
        "ph" -> "Philippines"
        "id" -> "Indonesia"
        "au" -> "Úc"
        else -> iso.uppercase()
    }

    fun allCountries(): List<String> = ALL_CARRIERS.map { it.countryIso }.distinct()

    // Fix guide chung cho từng brand đã nằm trong BandCheckManager.getFixGuide
    // Dưới đây là map brand → danh sách bước chi tiết theo tài liệu tổng hợp
    fun fixGuidesForAllBrands(): Map<String, List<String>> = mapOf(
        "Xiaomi" to listOf("*#*#86583#*#* để hiện VoLTE", "*#*#869434#*#* cho VoWiFi", "Cài đặt → SIM → bật VoLTE"),
        "Pixel" to listOf("Shizuku + Pixel IMS full enable", "*#*#4636#*#* kiểm tra IMS"),
        "Samsung" to listOf("Cài đặt → Kết nối → Mạng di động → VoLTE", "*#0011# kiểm tra IMS", "*#2263# chọn band"),
        "Oppo/Realme/OnePlus" to listOf("*#800# EngineerMode", "Cài đặt → SIM → VoLTE"),
        "Sony/Asus/Nokia" to listOf("*#*#4636#*#* → VoLTE Provisioned"),
        "Generic" to listOf("*#*#4636#*#* kiểm tra chung")
    )
}
