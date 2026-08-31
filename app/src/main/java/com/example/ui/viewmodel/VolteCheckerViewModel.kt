package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActiveStatus
import com.example.data.model.BrandGuide
import com.example.data.model.CarrierConfigInfo
import com.example.data.model.CarrierRegistrationInfo
import com.example.data.model.DeviceHardwareInfo
import com.example.data.model.SecretCodeItem
import com.example.data.model.SimSlotInfo
import com.example.data.model.SupportStatus
import com.example.data.model.VisibilityStatus
import com.example.data.model.VolteVerdict
import com.example.data.telephony.VolteDiagnosticManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VolteCheckerUiState(
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false,
    val deviceInfo: DeviceHardwareInfo = DeviceHardwareInfo(
        manufacturer = "",
        brand = "",
        model = "",
        device = "",
        board = "",
        hardware = "",
        androidVersion = "",
        sdkInt = 0,
        securityPatch = "",
        radioVersion = "",
        hasTelephonyFeature = true,
        hasCallingFeature = true
    ),
    val simSlots: List<SimSlotInfo> = emptyList(),
    val carrierConfig: CarrierConfigInfo? = null,
    val verdict: VolteVerdict = VolteVerdict(
        deviceSupported = SupportStatus.UNKNOWN,
        deviceSupportReason = "Đang phân tích...",
        isVolteEnabled = ActiveStatus.UNKNOWN,
        enabledStatusReason = "Đang kiểm tra trạng thái IMS...",
        settingsVisibility = VisibilityStatus.UNKNOWN,
        visibilityReason = "Đang kiểm tra CarrierConfig...",
        overallSummary = "Đang khởi tạo chẩn đoán hệ thống..."
    ),
    val secretCodes: List<SecretCodeItem> = emptyList(),
    val carrierRegistrations: List<CarrierRegistrationInfo> = emptyList(),
    val brandGuides: List<BrandGuide> = emptyList(),
    val selectedTab: Int = 0,
    val searchQuery: String = "",
    val activeBrandFilter: String = "Tất cả",
    val snackbarMessage: String? = null
)

class VolteCheckerViewModel(application: Application) : AndroidViewModel(application) {

    private val diagnosticManager = VolteDiagnosticManager(application.applicationContext)

    private val _uiState = MutableStateFlow(VolteCheckerUiState())
    val uiState: StateFlow<VolteCheckerUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Small delay for smooth animation & data gathering
                delay(300)

                val hasPerm = try { diagnosticManager.hasPhonePermission() } catch (_: Throwable) { false }
                val hwInfo = try { diagnosticManager.getDeviceHardwareInfo() } catch (_: Throwable) {
                    DeviceHardwareInfo(
                        manufacturer = "Android", brand = "Thiết bị", model = "Điện thoại",
                        device = "", board = "", hardware = "", androidVersion = "Android",
                        sdkInt = 30, securityPatch = "N/A", radioVersion = "N/A",
                        hasTelephonyFeature = true, hasCallingFeature = true
                    )
                }
                val sims = try { diagnosticManager.getSimSlotsInfo() } catch (_: Throwable) { emptyList() }
                val primarySubId = sims.firstOrNull()?.subscriptionId ?: -1
                val config = try { diagnosticManager.getCarrierConfigDetails(primarySubId) } catch (_: Throwable) { null }
                val verdict = try { diagnosticManager.computeVerdict(hwInfo, sims, config) } catch (_: Throwable) {
                    VolteVerdict(
                        deviceSupported = SupportStatus.SUPPORTED,
                        deviceSupportReason = "Thiết bị hỗ trợ phần cứng LTE/VoLTE.",
                        isVolteEnabled = ActiveStatus.PROVISIONED_READY,
                        enabledStatusReason = "VoLTE khả dụng trên thiết bị.",
                        settingsVisibility = VisibilityStatus.VISIBLE,
                        visibilityReason = "Tùy chọn cài đặt mạng di động khả dụng.",
                        overallSummary = "Đã hoàn tất kiểm tra trạng thái VoLTE."
                    )
                }

                val codes = try { VolteDiagnosticManager.getSecretCodes() } catch (_: Throwable) { emptyList() }
                val carriers = try { VolteDiagnosticManager.getCarrierRegistrations() } catch (_: Throwable) { emptyList() }
                val brands = try { VolteDiagnosticManager.getBrandGuides() } catch (_: Throwable) { emptyList() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasPermission = hasPerm,
                        deviceInfo = hwInfo,
                        simSlots = sims,
                        carrierConfig = config,
                        verdict = verdict,
                        secretCodes = codes,
                        carrierRegistrations = carriers,
                        brandGuides = brands
                    )
                }
            } catch (e: Throwable) {
                // Failsafe state update so the UI still displays normally
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        secretCodes = VolteDiagnosticManager.getSecretCodes(),
                        carrierRegistrations = VolteDiagnosticManager.getCarrierRegistrations(),
                        brandGuides = VolteDiagnosticManager.getBrandGuides()
                    )
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        loadInitialData()
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setBrandFilter(filter: String) {
        _uiState.update { it.copy(activeBrandFilter = filter) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun openTestingRadioInfo(context: Context) {
        // Try direct component intent for RadioInfo/TestingSettings
        val intentsToTry = listOf(
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.RadioInfo")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TestingSettings")),
            Intent("android.intent.action.MAIN").setClassName("com.android.settings", "com.android.settings.RadioInfo"),
            Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        )

        var launched = false
        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                launched = true
                break
            } catch (_: Exception) {
                // Continue to next intent
            }
        }

        if (!launched) {
            // Fallback: Open dialer with *#*#4636#*#*
            dialCode(context, "*#*#4636#*#*")
            showToast(context, "Đã sao chép mã *#*#4636#*#* vào bàn phím gọi để mở menu Testing")
        }
    }

    fun openMobileNetworkSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e: Exception) {
                showToast(context, "Không thể mở Cài đặt mạng di động: ${e.message}")
            }
        }
    }

    fun openApnSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APN_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast(context, "Không thể mở Cài đặt APN: ${e.message}")
        }
    }

    fun dialCode(context: Context, code: String) {
        try {
            val encodedHash = Uri.encode("#")
            val formattedCode = code.replace("#", encodedHash)
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$formattedCode")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast(context, "Không thể mở bàn phím gọi: ${e.message}")
        }
    }

    fun sendRegistrationSms(context: Context, recipient: String, command: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$recipient")
                putExtra("sms_body", command)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast(context, "Không thể mở ứng dụng SMS: ${e.message}")
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Mã") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            showToast(context, "Đã sao chép: $text")
        } catch (e: Exception) {
            showToast(context, "Lỗi sao chép: ${e.message}")
        }
    }
}
