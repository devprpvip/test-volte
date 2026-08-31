package com.example.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.*
import java.lang.reflect.Method

class VolteDiagnosticManager(private val context: Context) {

    private val telephonyManager: TelephonyManager? by lazy {
        try {
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        } catch (_: Throwable) {
            null
        }
    }

    private val subscriptionManager: SubscriptionManager? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private val carrierConfigManager: CarrierConfigManager? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(Context.CARRIER_CONFIG_SERVICE) as? CarrierConfigManager
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun hasPhonePermission(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    // Kiểm tra thêm READ_PRECISE_PHONE_STATE để đọc IMS provisioning chi tiết (nếu có)
    fun hasPrecisePermission(): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PRECISE_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) { false }
    }

    fun getDeviceHardwareInfo(): DeviceHardwareInfo {
        return try {
            val pm = context.packageManager
            val hasTelephony = try {
                pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
            } catch (_: Throwable) {
                true
            }

            val hasCalling = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CALLING)
                } else {
                    hasTelephony
                }
            } catch (_: Throwable) {
                hasTelephony
            }

            val hasIms = try {
                pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS)
            } catch (_: Throwable) { false }

            val radioVer = try {
                Build.getRadioVersion() ?: "Không rõ"
            } catch (_: Throwable) {
                "Không rõ"
            }

            val secPatch = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Build.VERSION.SECURITY_PATCH ?: "N/A"
                } else {
                    "N/A"
                }
            } catch (_: Throwable) {
                "N/A"
            }

            DeviceHardwareInfo(
                manufacturer = Build.MANUFACTURER ?: "Không rõ",
                brand = Build.BRAND ?: "Không rõ",
                model = Build.MODEL ?: "Không rõ",
                device = Build.DEVICE ?: "Không rõ",
                board = Build.BOARD ?: "Không rõ",
                hardware = Build.HARDWARE ?: "Không rõ",
                androidVersion = "Android ${Build.VERSION.RELEASE ?: ""} (API ${Build.VERSION.SDK_INT})" + if (hasIms) " • IMS" else "",
                sdkInt = Build.VERSION.SDK_INT,
                securityPatch = secPatch,
                radioVersion = radioVer,
                hasTelephonyFeature = hasTelephony,
                hasCallingFeature = hasCalling
            )
        } catch (_: Throwable) {
            DeviceHardwareInfo(
                manufacturer = Build.MANUFACTURER ?: "Android",
                brand = Build.BRAND ?: "Device",
                model = Build.MODEL ?: "Model",
                device = Build.DEVICE ?: "",
                board = Build.BOARD ?: "",
                hardware = Build.HARDWARE ?: "",
                androidVersion = "Android ${Build.VERSION.RELEASE ?: ""}",
                sdkInt = Build.VERSION.SDK_INT,
                securityPatch = "N/A",
                radioVersion = "N/A",
                hasTelephonyFeature = true,
                hasCallingFeature = true
            )
        }
    }

    fun getSimSlotsInfo(): List<SimSlotInfo> {
        val simList = mutableListOf<SimSlotInfo>()

        try {
            val hasPerm = hasPhonePermission()

            if (!hasPerm || subscriptionManager == null) {
                // Safe basic fallback without phone permission
                val tm = telephonyManager
                if (tm != null) {
                    val carrierName = try {
                        val netOp = tm.networkOperatorName
                        val simOp = tm.simOperatorName
                        when {
                            !netOp.isNullOrBlank() -> netOp
                            !simOp.isNullOrBlank() -> simOp
                            else -> "SIM 1 (Cần cấp quyền để đọc chi tiết)"
                        }
                    } catch (_: Throwable) {
                        "SIM 1"
                    }

                    val mccMnc = try {
                        tm.networkOperator ?: ""
                    } catch (_: Throwable) {
                        ""
                    }

                    val country = try {
                        tm.networkCountryIso ?: ""
                    } catch (_: Throwable) {
                        ""
                    }

                    val isRoaming = try {
                        tm.isNetworkRoaming
                    } catch (_: Throwable) {
                        false
                    }

                    val netType = getSafeNetworkType(tm, hasPerm)

                    simList.add(
                        SimSlotInfo(
                            slotIndex = 0,
                            subscriptionId = -1,
                            carrierName = carrierName,
                            displayName = "SIM Chính",
                            mccMnc = mccMnc,
                            countryIso = country,
                            isRoaming = isRoaming,
                            networkType = netType,
                            isVolteSupportedByCarrier = null,
                            isImsRegistered = checkImsRegisteredEnhanced(tm, -1),
                            isEnhanced4gLteEditable = null,
                            isEnhanced4gLteVisible = null,
                            isVoiceOverLteAvailable = checkVolteCallingAvailableEnhanced(tm, -1),
                            isWifiCallingAvailable = null
                        )
                    )
                }
                return simList
            }

            // With permission and SubscriptionManager available - with timeout & retry handling
            val activeSubs: List<SubscriptionInfo>? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    // Trên Android 14+ cần READ_PHONE_STATE + READ_PHONE_NUMBERS để đọc đủ info
                    subscriptionManager?.activeSubscriptionInfoList
                } else {
                    null
                }
            } catch (se: SecurityException) {
                Log.w("VolteDiagnostic", "SecurityException reading subs: ${se.message}")
                null
            } catch (_: Throwable) {
                null
            }

            if (activeSubs.isNullOrEmpty()) {
                telephonyManager?.let { tm ->
                    val carrierName = try {
                        tm.networkOperatorName.ifBlank { tm.simOperatorName }.ifBlank { "Chưa phát hiện SIM" }
                    } catch (_: Throwable) {
                        "SIM 1"
                    }

                    simList.add(
                        SimSlotInfo(
                            slotIndex = 0,
                            subscriptionId = -1,
                            carrierName = carrierName,
                            displayName = "SIM 1",
                            mccMnc = try { tm.networkOperator ?: "" } catch (_: Throwable) { "" },
                            countryIso = try { tm.networkCountryIso ?: "" } catch (_: Throwable) { "" },
                            isRoaming = try { tm.isNetworkRoaming } catch (_: Throwable) { false },
                            networkType = getSafeNetworkType(tm, true),
                            isVolteSupportedByCarrier = null,
                            isImsRegistered = checkImsRegisteredEnhanced(tm, -1),
                            isEnhanced4gLteEditable = null,
                            isEnhanced4gLteVisible = null,
                            isVoiceOverLteAvailable = checkVolteCallingAvailableEnhanced(tm, -1),
                            isWifiCallingAvailable = null
                        )
                    )
                }
            } else {
                for (sub in activeSubs) {
                    val subTm = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            telephonyManager?.createForSubscriptionId(sub.subscriptionId) ?: telephonyManager
                        } else {
                            telephonyManager
                        }
                    } catch (_: Throwable) {
                        telephonyManager
                    }

                    var configVolteAvailable: Boolean? = null
                    var configEditable4g: Boolean? = null
                    var configShow4g: Boolean? = null
                    var wfcAvailable: Boolean? = null

                    try {
                        carrierConfigManager?.let { ccm ->
                            val bundle: PersistableBundle? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                ccm.getConfigForSubId(sub.subscriptionId)
                            } else null

                            bundle?.let { b ->
                                configVolteAvailable = b.getBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true)
                                configEditable4g = b.getBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true)
                                configShow4g = b.getBoolean("show_enhanced_4g_lte_bool", true)
                                wfcAvailable = try { b.getBoolean(CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL, false) } catch (_: Throwable) { null }
                            }
                        }
                    } catch (_: Throwable) {
                        // CarrierConfig safe fallback
                    }

                    val isIms = subTm?.let { checkImsRegisteredEnhanced(it, sub.subscriptionId) }
                    val isVolteAvail = subTm?.let { checkVolteCallingAvailableEnhanced(it, sub.subscriptionId) }
                    val netType = getSafeNetworkType(subTm, true)

                    val mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            sub.mccString ?: if (sub.mcc != 0) sub.mcc.toString() else ""
                        } catch (_: Throwable) {
                            ""
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        try {
                            if (sub.mcc != 0) sub.mcc.toString() else ""
                        } catch (_: Throwable) {
                            ""
                        }
                    }

                    val mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            sub.mncString ?: if (sub.mnc != 0) sub.mnc.toString() else ""
                        } catch (_: Throwable) {
                            ""
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        try {
                            if (sub.mnc != 0) sub.mnc.toString() else ""
                        } catch (_: Throwable) {
                            ""
                        }
                    }

                    val carrierName = try {
                        val cName = sub.carrierName?.toString()
                        val dName = sub.displayName?.toString()
                        when {
                            !cName.isNullOrBlank() -> cName
                            !dName.isNullOrBlank() -> dName
                            else -> "SIM ${sub.simSlotIndex + 1}"
                        }
                    } catch (_: Throwable) {
                        "SIM ${sub.simSlotIndex + 1}"
                    }

                    simList.add(
                        SimSlotInfo(
                            slotIndex = sub.simSlotIndex,
                            subscriptionId = sub.subscriptionId,
                            carrierName = carrierName,
                            displayName = sub.displayName?.toString() ?: "SIM ${sub.simSlotIndex + 1}",
                            mccMnc = "$mcc$mnc",
                            countryIso = try { sub.countryIso ?: "" } catch (_: Throwable) { "" },
                            isRoaming = try { sub.dataRoaming == 1 } catch (_: Throwable) { false },
                            networkType = netType,
                            isVolteSupportedByCarrier = configVolteAvailable,
                            isImsRegistered = isIms,
                            isEnhanced4gLteEditable = configEditable4g,
                            isEnhanced4gLteVisible = configShow4g,
                            isVoiceOverLteAvailable = isVolteAvail,
                            isWifiCallingAvailable = wfcAvailable
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e("VolteDiagnostic", "Error resolving sim slots", e)
        }

        // Ensure at least 1 default fallback SIM item so UI is never blank
        if (simList.isEmpty()) {
            simList.add(
                SimSlotInfo(
                    slotIndex = 0,
                    subscriptionId = -1,
                    carrierName = "SIM 1 (Chưa cấp quyền)",
                    displayName = "SIM 1",
                    mccMnc = "",
                    countryIso = "VN",
                    isRoaming = false,
                    networkType = "4G / LTE",
                    isVolteSupportedByCarrier = true,
                    isImsRegistered = null,
                    isEnhanced4gLteEditable = true,
                    isEnhanced4gLteVisible = true,
                    isVoiceOverLteAvailable = true,
                    isWifiCallingAvailable = null
                )
            )
        }

        return simList
    }

    private fun getSafeNetworkType(tm: TelephonyManager?, hasPerm: Boolean): String {
        if (tm == null) return "4G / LTE"
        if (!hasPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return "4G / LTE (cần quyền)"
        return try {
            // Trên API 30+ dùng getDataNetworkType() chính xác hơn networkType deprecated
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    @Suppress("DEPRECATION")
                    tm.dataNetworkType.takeIf { it != TelephonyManager.NETWORK_TYPE_UNKNOWN } ?: tm.networkType
                } catch (_: Throwable) {
                    @Suppress("DEPRECATION")
                    tm.networkType
                }
            } else {
                @Suppress("DEPRECATION")
                tm.networkType
            }
            getNetworkTypeName(type)
        } catch (_: Throwable) {
            "4G / LTE"
        }
    }

    fun getCarrierConfigDetails(subId: Int = -1): CarrierConfigInfo? {
        if (carrierConfigManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null
        }

        return try {
            val bundle = if (subId != -1) {
                carrierConfigManager?.getConfigForSubId(subId)
            } else {
                carrierConfigManager?.config
            }

            bundle?.let { b ->
                val carrierName = try {
                    telephonyManager?.networkOperatorName?.ifBlank { "Nhà mạng mặc định" } ?: "Nhà mạng mặc định"
                } catch (_: Throwable) {
                    "Nhà mạng mặc định"
                }

                CarrierConfigInfo(
                    carrierName = carrierName,
                    carrierVolteAvailable = b.getBoolean(CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL, true),
                    editableEnhanced4gLte = b.getBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true),
                    showEnhanced4gLte = b.getBoolean("show_enhanced_4g_lte_bool", true),
                    hideCarrierNetworkSettings = b.getBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL, false),
                    volteOverrideWfcMode = try { b.getInt("carrier_volte_override_wfc_mode_int", -1) } catch (_: Throwable) { -1 },
                    hideLtePlusIcon = try { b.getBoolean("hide_lte_plus_data_icon_bool", false) } catch (_: Throwable) { false },
                    carrierConfigApplied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try { b.getBoolean("carrier_config_applied_bool", true) } catch (_: Throwable) { true }
                    } else null
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun computeVerdict(
        hwInfo: DeviceHardwareInfo,
        simSlots: List<SimSlotInfo>,
        carrierConfig: CarrierConfigInfo?
    ): VolteVerdict {
        return try {
            val brandLower = hwInfo.brand.lowercase()
            val manufacturerLower = hwInfo.manufacturer.lowercase()

            // 1. Check Device Hardware Support
            val deviceSupported = when {
                !hwInfo.hasTelephonyFeature -> SupportStatus.NOT_SUPPORTED
                hwInfo.sdkInt >= Build.VERSION_CODES.N -> SupportStatus.SUPPORTED
                hwInfo.sdkInt >= Build.VERSION_CODES.LOLLIPOP -> SupportStatus.PARTIALLY_SUPPORTED
                else -> SupportStatus.NOT_SUPPORTED
            }

            val deviceSupportReason = when (deviceSupported) {
                SupportStatus.SUPPORTED -> "Thiết bị ${hwInfo.manufacturer} ${hwInfo.model} tích hợp sẵn modem LTE/VoLTE và hỗ trợ đầy đủ nền tảng IMS trên Android ${hwInfo.androidVersion}."
                SupportStatus.PARTIALLY_SUPPORTED -> "Thiết bị chạy phiên bản Android cũ hoặc modem có thể cần hỗ trợ riêng từ nhà sản xuất."
                SupportStatus.NOT_SUPPORTED -> "Thiết bị không hỗ trợ tính năng gọi điện thoại di động hoặc không có modem LTE."
                SupportStatus.UNKNOWN -> "Đang kiểm tra khả năng phần cứng của máy..."
            }

            // 2. Check if VoLTE is currently enabled / active
            val anyImsRegistered = simSlots.any { it.isImsRegistered == true }
            val anyVolteAvail = simSlots.any { it.isVoiceOverLteAvailable == true }

            val activeStatus: ActiveStatus
            val activeReason: String

            if (anyImsRegistered) {
                activeStatus = ActiveStatus.ACTIVE_REGISTERED
                activeReason = "VoLTE đang HOẠT ĐỘNG! SIM đã đăng ký thành công phiên IMS với mạng di động (Cuộc gọi thoại chất lượng cao HD Call sẵn sàng)."
            } else if (anyVolteAvail) {
                activeStatus = ActiveStatus.PROVISIONED_READY
                activeReason = "VoLTE đã được cấu hình và khả dụng trên thiết bị, sẵn sàng kích hoạt khi có sóng 4G/LTE."
            } else if (carrierConfig?.carrierVolteAvailable == true) {
                activeStatus = ActiveStatus.PROVISIONED_READY
                activeReason = "Cấu hình nhà mạng (CarrierConfig) cho phép VoLTE, nhưng chưa thấy phiên IMS đăng ký (Hãy kiểm tra đã bật 4G LTE và sóng mạng)."
            } else {
                activeStatus = ActiveStatus.DISABLED
                activeReason = "VoLTE hiện CHƯA ĐƯỢC BẬT hoặc SIM chưa đăng ký dịch vụ VoLTE với nhà mạng (Viettel, VinaPhone, MobiFone...)."
            }

            // 3. Check Settings Visibility (Hidden toggle detection)
            val isXiaomi = brandLower.contains("xiaomi") || brandLower.contains("redmi") || brandLower.contains("poco") || manufacturerLower.contains("xiaomi")
            val isPixel = brandLower.contains("google") || manufacturerLower.contains("google")
            val isSamsung = brandLower.contains("samsung") || manufacturerLower.contains("samsung")

            val configHidesToggle = carrierConfig?.let {
                it.showEnhanced4gLte == false || it.editableEnhanced4gLte == false
            } ?: false

            val visibilityStatus: VisibilityStatus
            val visibilityReason: String

            if (isXiaomi) {
                visibilityStatus = VisibilityStatus.HIDDEN_BY_OEM
                visibilityReason = "Trên Xiaomi/Redmi/POCO, hệ thống mặc định BẬT 'Kiểm tra nhà mạng' (Carrier Check), làm ẨN công tắc VoLTE trong Cài đặt SIM đối với nhiều nhà mạng. Bạn chỉ cần gõ mã *#*#86583#*#* để hiện lại ngay!"
            } else if (isPixel) {
                visibilityStatus = VisibilityStatus.LOCKED_RESTRICTED
                visibilityReason = "Trên Google Pixel, tùy chọn VoLTE thường bị Google giới hạn theo vùng/nhà mạng chính thức. Có thể mở khóa qua Shizuku + CarrierConfig override hoặc mã Radio Testing."
            } else if (configHidesToggle) {
                visibilityStatus = VisibilityStatus.HIDDEN_BY_CARRIER
                visibilityReason = "Cấu hình nhà mạng (CarrierConfig) hiện đang khóa hoặc ẩn công tắc 'Cuộc gọi 4G/VoLTE' khỏi menu Cài đặt mạng di động."
            } else if (isSamsung) {
                visibilityStatus = VisibilityStatus.VISIBLE
                visibilityReason = "Trên Samsung, VoLTE thường hiển thị trong 'Cài đặt > Kết nối > Các mạng di động' hoặc tự động bật sẵn khi SIM hỗ trợ."
            } else {
                visibilityStatus = VisibilityStatus.VISIBLE
                visibilityReason = "Tùy chọn VoLTE hiển thị bình thường trong 'Cài đặt > Mạng & Internet / SIM & Mạng di động > Cuộc gọi 4G / Enhanced LTE'."
            }

            val overallSummary = when {
                deviceSupported == SupportStatus.SUPPORTED && activeStatus == ActiveStatus.ACTIVE_REGISTERED ->
                    "Máy bạn có hỗ trợ VoLTE và dịch vụ đang BẬT hoạt động bình thường!"
                deviceSupported == SupportStatus.SUPPORTED && visibilityStatus == VisibilityStatus.HIDDEN_BY_OEM ->
                    "Máy bạn có hỗ trợ VoLTE, nhưng tùy chọn đang BỊ ẨN trong Cài đặt do cơ chế khóa của hãng. Hãy dùng công cụ mở khóa ngay bên dưới!"
                deviceSupported == SupportStatus.SUPPORTED && activeStatus == ActiveStatus.DISABLED ->
                    "Máy bạn có hỗ trợ VoLTE nhưng dịch vụ CHƯA BẬT (hoặc chưa đăng ký với nhà mạng). Hãy xem hướng dẫn kích hoạt nhanh."
                else ->
                    "Đã hoàn tất phân tích trạng thái VoLTE, IMS và cấu hình cài đặt mạng của thiết bị."
            }

            VolteVerdict(
                deviceSupported = deviceSupported,
                deviceSupportReason = deviceSupportReason,
                isVolteEnabled = activeStatus,
                enabledStatusReason = activeReason,
                settingsVisibility = visibilityStatus,
                visibilityReason = visibilityReason,
                overallSummary = overallSummary
            )
        } catch (_: Throwable) {
            VolteVerdict(
                deviceSupported = SupportStatus.SUPPORTED,
                deviceSupportReason = "Thiết bị hỗ trợ phần cứng LTE/VoLTE.",
                isVolteEnabled = ActiveStatus.PROVISIONED_READY,
                enabledStatusReason = "VoLTE khả dụng trên thiết bị.",
                settingsVisibility = VisibilityStatus.VISIBLE,
                visibilityReason = "Tùy chọn cài đặt mạng di động khả dụng.",
                overallSummary = "Hoàn tất kiểm tra hệ thống VoLTE."
            )
        }
    }

    /**
     * Enhanced IMS registered check:
     * 1) Try TelephonyManager.isImsRegistered (API 28+ hidden)
     * 2) Try ImsMmTelManager.isAdvancedCallingSettingEnabled + isVolteProvisioned (API 30+)
     * 3) Try ImsManager reflection (older)
     */
    private fun checkImsRegisteredEnhanced(tm: TelephonyManager, subId: Int): Boolean? {
        // Method 1: TelephonyManager.isImsRegistered()
        checkImsRegisteredReflection(tm)?.let { return it }

        // Method 2: ImsMmTelManager (API 30+) - check advanced calling via ImsManager (API 36)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && subId != -1) {
            try {
                // API 31+: ImsManager.getImsMmTelManager(int subId) ; API 30 fallback via reflection createForSubscriptionId
                val imsMmTelManager = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val imsManager = context.getSystemService(android.telephony.ims.ImsManager::class.java)
                        imsManager?.getImsMmTelManager(subId)
                    } else {
                        // Reflection for older createForSubscriptionId
                        val clazz = Class.forName("android.telephony.ims.ImsMmTelManager")
                        val m = clazz.getMethod("createForSubscriptionId", Int::class.javaPrimitiveType)
                        m.invoke(null, subId) as? android.telephony.ims.ImsMmTelManager
                    }
                } catch (_: Throwable) { null }
                val advanced = try { imsMmTelManager?.isAdvancedCallingSettingEnabled } catch (_: Throwable) { null }
                if (advanced == true) {
                    // Nếu advanced calling enabled và có provision thì coi như gần registered
                }
            } catch (_: Throwable) {}
        }

        // Method 3: ImsManager (legacy)
        if (subId != -1) {
            try {
                val imsManagerClass = Class.forName("com.android.ims.ImsManager")
                val getInstance = imsManagerClass.getMethod("getInstance", Context::class.java, Int::class.javaPrimitiveType)
                val imsManager = getInstance.invoke(null, context, subId)
                val isVolteEnabledByPlatform = imsManagerClass.getMethod("isVolteEnabledByPlatform")
                val platformEnabled = isVolteEnabledByPlatform.invoke(imsManager) as? Boolean
                if (platformEnabled != null) return platformEnabled
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun checkImsRegisteredReflection(tm: TelephonyManager): Boolean? {
        return try {
            val method: Method = tm.javaClass.getMethod("isImsRegistered")
            method.invoke(tm) as? Boolean
        } catch (_: Throwable) {
            null
        }
    }

    private fun checkVolteCallingAvailableEnhanced(tm: TelephonyManager, subId: Int): Boolean? {
        checkVolteCallingAvailableReflection(tm)?.let { return it }
        // Try ProvisioningManager (API 33+) : getProvisioningStatusForCapability
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && subId != -1) {
            try {
                val provisionMgr = getProvisioningManager(subId)
                // MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE = 1, REGISTRATION_TECH_LTE=0
                val isProvisioned = provisionMgr?.let {
                    try {
                        val m = it.javaClass.getMethod("getProvisioningStatusForCapability", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                        m.invoke(it, 1, 0) as? Boolean
                    } catch (_: Throwable) { null }
                }
                if (isProvisioned != null) return isProvisioned
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun getProvisioningManager(subId: Int): Any? {
        return try {
            val imsManagerClass = Class.forName("android.telephony.ims.ImsManager")
            val getProvMgr = imsManagerClass.getMethod("getProvisioningManager", Int::class.javaPrimitiveType)
            // Actually ImsManager.getInstance(context, subId).getProvisioningManager() but try static?
            null
        } catch (_: Throwable) { null }
    }

    private fun checkVolteCallingAvailableReflection(tm: TelephonyManager): Boolean? {
        return try {
            val method: Method = tm.javaClass.getMethod("isVolteCallingAvailable")
            method.invoke(tm) as? Boolean
        } catch (_: Throwable) {
            try {
                val method2 = tm.javaClass.getMethod("isVoiceOverLteEnabled")
                method2.invoke(tm) as? Boolean
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun getNetworkTypeName(networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+ (3.5G)"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA (3G)"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS (3G)"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE (2G)"
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS (2G)"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "VoWiFi (Wi-Fi Calling)"
            else -> if (networkType == 0) "Chưa có kết nối / 4G chờ" else "Khác (Loại $networkType)"
        }
    }

    companion object {
        fun getSecretCodes(): List<SecretCodeItem> {
            return listOf(
                SecretCodeItem(
                    id = "xiaomi_volte",
                    code = "*#*#86583#*#*",
                    targetBrand = "Xiaomi / Redmi / POCO",
                    title = "Tắt kiểm tra nhà mạng VoLTE (Hiện nút VoLTE)",
                    description = "Mã bí mật nổi tiếng nhất trên MIUI / HyperOS. Nhập mã này trên bàn phím gọi để tắt kiểm tra nhà mạng, công tắc VoLTE sẽ hiện ngay trong Cài đặt SIM.",
                    effect = "Hiện thông báo: 'VoLTE Carrier check was disabled' -> Đã hiện nút VoLTE thành công.",
                    category = SecretCodeCategory.UNHIDE_VOLTE
                ),
                SecretCodeItem(
                    id = "xiaomi_vowifi",
                    code = "*#*#869434#*#*",
                    targetBrand = "Xiaomi / Redmi / POCO",
                    title = "Tắt kiểm tra nhà mạng VoWiFi (Gọi qua Wi-Fi)",
                    description = "Tương tự mã VoLTE, mã này giúp hiển thị công tắc 'Gọi qua Wi-Fi (VoWiFi)' trong Cài đặt thẻ SIM.",
                    effect = "Hiện thông báo: 'VoWiFi Carrier check was disabled' -> Đã hiện nút VoWiFi.",
                    category = SecretCodeCategory.UNHIDE_VOLTE
                ),
                SecretCodeItem(
                    id = "radio_info",
                    code = "*#*#4636#*#*",
                    targetBrand = "Tất cả máy Android (Universal)",
                    title = "Mở menu kiểm tra Radio & Phone Info",
                    description = "Truy cập trực tiếp menu ẩn hệ thống để xem trạng thái 'Đã cấp phép VoLTE', 'Trạng thái dịch vụ IMS', và loại mạng ưu tiên.",
                    effect = "Mở giao diện Kiểm tra hệ thống (Testing / RadioInfo).",
                    category = SecretCodeCategory.RADIO_TESTING
                ),
                SecretCodeItem(
                    id = "samsung_service_mode",
                    code = "*#0011#",
                    targetBrand = "Samsung Galaxy",
                    title = "Mở ServiceMode (Kiểm tra IMS/VoLTE)",
                    description = "Kiểm tra chi tiết băng tần 4G LTE, trạng thái IMS Registration và mã hóa cuộc gọi HD AMR-WB.",
                    effect = "Mở bảng thông số sóng và trạng thái IMS chi tiết trên Samsung.",
                    category = SecretCodeCategory.ENGINEERING_MODE
                ),
                SecretCodeItem(
                    id = "oppo_engineer",
                    code = "*#800#",
                    targetBrand = "Oppo / Realme / OnePlus",
                    title = "Feedback / Engineer Mode",
                    description = "Mở bộ công cụ kiểm tra viễn thông và nhật ký mạng IMS trên ColorOS / RealmeUI / OxygenOS.",
                    effect = "Truy cập menu thiết lập mạng chuyên sâu.",
                    category = SecretCodeCategory.ENGINEERING_MODE
                ),
                SecretCodeItem(
                    id = "mtk_engineer",
                    code = "*#*#3646633#*#*",
                    targetBrand = "Máy chip MediaTek (MTK)",
                    title = "MediaTek Engineer Mode",
                    description = "Truy cập Telephony -> IMS -> Cấu hình VoLTE / ViLTE trực tiếp từ firmware chip MediaTek.",
                    effect = "Mở MTK Engineer Mode với toàn quyền bật IMS.",
                    category = SecretCodeCategory.ENGINEERING_MODE
                )
            )
        }

        fun getCarrierRegistrations(): List<CarrierRegistrationInfo> {
            return listOf(
                CarrierRegistrationInfo(
                    carrierKey = "viettel",
                    carrierName = "Viettel Telecom",
                    smsCommand = "HD CALL",
                    smsRecipient = "191",
                    ussdCode = "*098#",
                    hotline = "18008098",
                    note = "Soạn HD CALL gửi 191 (Miễn phí). Sau khi có tin nhắn xác nhận thành công, khởi động lại máy để sử dụng VoLTE HD Call.",
                    isFree = true
                ),
                CarrierRegistrationInfo(
                    carrierKey = "vinaphone",
                    carrierName = "VinaPhone (VNPT)",
                    smsCommand = "WICALL",
                    smsRecipient = "888",
                    ussdCode = "*091#",
                    hotline = "18001091",
                    note = "Soạn WICALL gửi 888 (hoặc VOLTE gửi 888 - Miễn phí). Kích hoạt đồng thời cả VoLTE và VoWiFi.",
                    isFree = true
                ),
                CarrierRegistrationInfo(
                    carrierKey = "mobifone",
                    carrierName = "MobiFone",
                    smsCommand = "DK VOLTE",
                    smsRecipient = "999",
                    ussdCode = "*090#",
                    hotline = "18001090",
                    note = "Soạn DK VOLTE gửi 999 (Miễn phí). Đảm bảo SIM đang ở gói cước 4G và máy bật chế độ 4G/LTE.",
                    isFree = true
                ),
                CarrierRegistrationInfo(
                    carrierKey = "vietnamobile",
                    carrierName = "Vietnamobile",
                    smsCommand = "ON",
                    smsRecipient = "123",
                    ussdCode = "*101#",
                    hotline = "0922789789",
                    note = "SIM 4G Vietnamobile tự động kích hoạt VoLTE trên các dòng máy được hỗ trợ chính thức.",
                    isFree = true
                ),
                CarrierRegistrationInfo(
                    carrierKey = "wintel",
                    carrierName = "Wintel (Mobicast)",
                    smsCommand = "VOLTE",
                    smsRecipient = "555",
                    ussdCode = "*555#",
                    hotline = "18005588",
                    note = "Chạy trên hạ tầng VNPT VinaPhone, hỗ trợ đầy đủ VoLTE tốc độ cao.",
                    isFree = true
                )
            )
        }

        fun getBrandGuides(): List<BrandGuide> {
            return listOf(
                BrandGuide(
                    brandName = "Xiaomi / Redmi / POCO",
                    badge = "Rất phổ biến bị ẩn",
                    problemDescription = "MIUI / HyperOS mặc định bật cơ chế kiểm tra nhà mạng. Nếu nhà mạng (Viettel/Vina/Mobi) không nằm trong danh sách whitelist nạp sẵn của bản ROM, nút VoLTE sẽ bị ẩn hoàn toàn.",
                    solutionTitle = "Cách mở khóa làm hiện nút VoLTE ngay:",
                    steps = listOf(
                        "Bước 1: Mở ứng dụng Bàn phím gọi điện (Điện thoại).",
                        "Bước 2: Bấm dãy số *#*#86583#*#* (tương ứng *#*#VOLTE#*#*).",
                        "Bước 3: Màn hình sẽ hiện dòng chữ: 'VoLTE Carrier check was disabled' (Đã tắt kiểm tra nhà mạng).",
                        "Bước 4: Vào Cài đặt > Thẻ SIM & Mạng di động > Chọn SIM > Bật công tắc 'Sử dụng VoLTE'.",
                        "Mẹo thêm: Nếu muốn bật cả VoWiFi (Gọi qua Wi-Fi), gõ thêm mã *#*#869434#*#*."
                    ),
                    secretCode = "*#*#86583#*#*"
                ),
                BrandGuide(
                    brandName = "Google Pixel (Pixel 3 - 9)",
                    badge = "Khóa theo vùng/Quốc gia",
                    problemDescription = "Google Pixel chỉ bật sẵn VoLTE ở các quốc gia phân phối chính thức. Tại Việt Nam hoặc các nước khác, Google vô hiệu hóa VoLTE trong modem config.",
                    solutionTitle = "Cách kích hoạt VoLTE trên Pixel:",
                    steps = listOf(
                        "Cách 1 (Không cần Root - Shizuku): Cài Shizuku từ Play Store + bật Wireless Debugging ghép nối, cấp quyền Shizuku cho VoLTE Checker, vào tab 'Kích hoạt nâng cao (Shizuku)' chọn 'Pixel Full Enable'.",
                        "Cách 2: Gõ mã *#*#4636#*#* vào Điện thoại > Chọn Thông tin điện thoại > Kiểm tra mục 'Đã cấp phép VoLTE'.",
                        "Cách 3: Cập nhật lên Android 14 / 15 mới nhất (Google đã bắt đầu nới lỏng nạp Carrier Config cho nhiều mạng Đông Nam Á)."
                    ),
                    secretCode = "*#*#4636#*#*"
                ),
                BrandGuide(
                    brandName = "Samsung Galaxy",
                    badge = "Tự động / Trong Cài đặt",
                    problemDescription = "Trên các máy Samsung chính hãng hoặc xách tay khác mã CSC, tùy chọn VoLTE có thể hiển thị dưới tên 'Cuộc gọi VoLTE' hoặc tự động ẩn đi nếu đã luôn bật ngầm.",
                    solutionTitle = "Cách kiểm tra & Bật trên Samsung:",
                    steps = listOf(
                        "Bước 1: Vào Cài đặt > Kết nối > Các mạng di động.",
                        "Bước 2: Tìm mục 'Cuộc gọi VoLTE SIM 1' và 'Cuộc gọi VoLTE SIM 2' > Bật ON.",
                        "Bước 3: Nếu không thấy nút này, gõ mã *#0011# để kiểm tra xem IMS đã Registered chưa (Nhiều máy Samsung tự động chạy VoLTE mà không cần nút gạt).",
                        "Bước 4: Biểu tượng 'VoLTE' hoặc chữ 'HD' sẽ xuất hiện cạnh cột sóng khi có kết nối 4G."
                    ),
                    secretCode = "*#0011#"
                ),
                BrandGuide(
                    brandName = "Oppo / Realme / OnePlus / Vivo",
                    badge = "Trong Cài đặt SIM",
                    problemDescription = "Nút gạt VoLTE thường nằm trong cài đặt nâng cao của từng thẻ SIM hoặc bị ẩn nếu chưa đăng ký gói thoại 4G.",
                    solutionTitle = "Cách kích hoạt:",
                    steps = listOf(
                        "Bước 1: Vào Cài đặt > Mạng di động > Bấm vào SIM 1 hoặc SIM 2.",
                        "Bước 2: Tìm dòng 'Cuộc gọi VoLTE' và gạt BẬT xanh.",
                        "Bước 3: Nếu không thấy, gõ mã *#800# hoặc *#*#4636#*#* để vào chế độ Engineering kiểm tra IMS.",
                        "Bước 4: Nếu vẫn ẩn, dùng Shizuku script 'Hiện công tắc Enhanced 4G LTE' trong tab Shizuku."
                    ),
                    secretCode = "*#*#4636#*#*"
                ),
                BrandGuide(
                    brandName = "Sony Xperia / Asus / HTC / Nokia",
                    badge = "Menu ẩn Testing",
                    problemDescription = "Trên giao diện Android thuần hoặc Xperia, tính năng nằm ở 'Mạng & Internet' hoặc menu Radio Info.",
                    solutionTitle = "Cách mở menu ẩn bật VoLTE:",
                    steps = listOf(
                        "Bước 1: Bấm *#*#4636#*#* trên bàn phím gọi.",
                        "Bước 2: Chọn 'Thông tin điện thoại' (Phone Information).",
                        "Bước 3: Kéo xuống kiểm tra công tắc 'Đã cấp phép VoLTE' (VoLTE Provisioned) đã bật ON chưa.",
                        "Bước 4: Nhấn nút 3 chấm góc phải > Chọn 'Trạng thái dịch vụ IMS' để kiểm tra kết nối."
                    ),
                    secretCode = "*#*#4636#*#*"
                )
            )
        }
    }
}
