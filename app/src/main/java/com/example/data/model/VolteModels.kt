package com.example.data.model

enum class SupportStatus {
    SUPPORTED,
    NOT_SUPPORTED,
    PARTIALLY_SUPPORTED,
    UNKNOWN
}

enum class ActiveStatus {
    ACTIVE_REGISTERED,
    PROVISIONED_READY,
    DISABLED,
    NOT_PROVISIONED,
    UNKNOWN
}

enum class VisibilityStatus {
    VISIBLE,
    HIDDEN_BY_CARRIER,
    HIDDEN_BY_OEM,
    LOCKED_RESTRICTED,
    UNKNOWN
}

data class VolteVerdict(
    val deviceSupported: SupportStatus,
    val deviceSupportReason: String,
    val isVolteEnabled: ActiveStatus,
    val enabledStatusReason: String,
    val settingsVisibility: VisibilityStatus,
    val visibilityReason: String,
    val overallSummary: String
)

data class SimSlotInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val displayName: String,
    val mccMnc: String,
    val countryIso: String,
    val isRoaming: Boolean,
    val networkType: String,
    val isVolteSupportedByCarrier: Boolean?,
    val isImsRegistered: Boolean?,
    val isEnhanced4gLteEditable: Boolean?,
    val isEnhanced4gLteVisible: Boolean?,
    val isVoiceOverLteAvailable: Boolean?,
    val isWifiCallingAvailable: Boolean?
)

data class DeviceHardwareInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val board: String,
    val hardware: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val radioVersion: String,
    val hasTelephonyFeature: Boolean,
    val hasCallingFeature: Boolean
)

data class CarrierConfigInfo(
    val carrierName: String,
    val carrierVolteAvailable: Boolean?,
    val editableEnhanced4gLte: Boolean?,
    val showEnhanced4gLte: Boolean?,
    val hideCarrierNetworkSettings: Boolean?,
    val volteOverrideWfcMode: Int?,
    val hideLtePlusIcon: Boolean?,
    val carrierConfigApplied: Boolean?
)

data class SecretCodeItem(
    val id: String,
    val code: String,
    val targetBrand: String, // e.g. "Xiaomi / Redmi / POCO", "Universal / AOSP", "Samsung", "Pixel", "Oppo / Realme / OnePlus"
    val title: String,
    val description: String,
    val effect: String,
    val category: SecretCodeCategory
)

enum class SecretCodeCategory {
    UNHIDE_VOLTE,
    RADIO_TESTING,
    ENGINEERING_MODE,
    CARRIER_PROVISIONING
}

data class CarrierRegistrationInfo(
    val carrierKey: String,
    val carrierName: String,
    val smsCommand: String,
    val smsRecipient: String,
    val ussdCode: String?,
    val hotline: String,
    val note: String,
    val isFree: Boolean = true
)

data class BrandGuide(
    val brandName: String,
    val badge: String,
    val problemDescription: String,
    val solutionTitle: String,
    val steps: List<String>,
    val secretCode: String? = null
)
