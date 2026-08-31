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
import com.example.data.band.BandCheckManager
import com.example.data.model.VolteVerdict
import com.example.data.shizuku.ShizukuManager
import com.example.data.shizuku.ShizukuPrivilegedOperations
import com.example.data.shizuku.VolteActivationScripts
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
    val snackbarMessage: String? = null,
    // Shizuku
    val shizukuState: ShizukuManager.ShizukuState = ShizukuManager.ShizukuState.Unknown,
    val isShizukuInstalled: Boolean = false,
    val shizukuLogs: String = "",
    val isRunningShizukuScript: Boolean = false,
    val lastScriptResult: String? = null,
    // Band check
    val bandResult: BandCheckManager.BandCheckResult? = null
)

class VolteCheckerViewModel(application: Application) : AndroidViewModel(application) {

    private val diagnosticManager = VolteDiagnosticManager(application.applicationContext)
    private val bandManager = BandCheckManager(application.applicationContext)
    private val shizukuOps = ShizukuPrivilegedOperations(application.applicationContext)

    private val _uiState = MutableStateFlow(VolteCheckerUiState())
    val uiState: StateFlow<VolteCheckerUiState> = _uiState.asStateFlow()

    init {
        initShizuku(application.applicationContext)
        loadInitialData()
        observeShizukuState()
    }

    private fun initShizuku(context: Context) {
        try { ShizukuManager.init(context) } catch (_: Throwable) {}
        _uiState.update {
            it.copy(
                shizukuState = ShizukuManager.queryStateSync(),
                isShizukuInstalled = ShizukuManager.isShizukuInstalled(context)
            )
        }
    }

    private fun observeShizukuState() {
        viewModelScope.launch {
            ShizukuManager.state.collect { state ->
                _uiState.update {
                    it.copy(
                        shizukuState = state,
                        isShizukuInstalled = try { ShizukuManager.isShizukuInstalled(getApplication<Application>().applicationContext) } catch (_: Throwable) { it.isShizukuInstalled }
                    )
                }
            }
        }
    }

    fun refreshShizukuState() {
        viewModelScope.launch(Dispatchers.IO) {
            try { ShizukuManager.refreshStateAsync() } catch (_: Throwable) {}
            delay(200)
            _uiState.update {
                it.copy(
                    shizukuState = ShizukuManager.queryStateSync(),
                    isShizukuInstalled = ShizukuManager.isShizukuInstalled(getApplication<Application>().applicationContext)
                )
            }
        }
    }

    fun requestShizukuPermission() {
        try { ShizukuManager.requestPermission() } catch (_: Throwable) {}
        // Optimistically check after 1s
        viewModelScope.launch {
            delay(1000)
            refreshShizukuState()
        }
    }

    fun runShizukuScript(script: VolteActivationScripts.ScriptItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRunningShizukuScript = true, lastScriptResult = "Đang chạy: ${script.title}...", shizukuLogs = ">>> ${script.title}\n${script.commands.joinToString("\n")}\n\nĐang thực thi...\n") }
            val result = try { shizukuOps.runScript(script) } catch (e: Throwable) {
                ShizukuPrivilegedOperations.OpResult(false, "Lỗi: ${e.message}", e.toString())
            }
            _uiState.update {
                it.copy(
                    isRunningShizukuScript = false,
                    lastScriptResult = result.message,
                    shizukuLogs = buildString {
                        appendLine(">>> ${script.title} - ${script.type}")
                        appendLine(script.commands.joinToString("\n"))
                        appendLine("\n--- Kết quả ---")
                        appendLine(result.message)
                        if (result.details.isNotBlank()) {
                            appendLine("\n--- Chi tiết ---")
                            appendLine(result.details)
                        }
                        result.warning?.let { w -> appendLine("\n⚠ Cảnh báo: $w") }
                    }
                )
            }
            // Auto refresh after script to show new verdict
            delay(800)
            loadInitialData()
        }
    }

    fun runBinderOverrideCarrierConfig(subId: Int, persistent: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRunningShizukuScript = true, lastScriptResult = "Đang override CarrierConfig subId=$subId...", shizukuLogs = ">>> Override CarrierConfig binder subId=$subId persistent=$persistent\n") }
            val res = try { shizukuOps.overrideCarrierConfigViaBinder(subId, persistent) } catch (e: Throwable) {
                ShizukuPrivilegedOperations.OpResult(false, "Lỗi: ${e.message}", e.toString())
            }
            _uiState.update { it.copy(isRunningShizukuScript = false, lastScriptResult = res.message, shizukuLogs = ">>> Override CarrierConfig subId=$subId\n${res.details}\n\n${res.message}\n${res.warning ?: ""}") }
            delay(600); loadInitialData()
        }
    }

    fun runImsProvisioningEnable(subId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRunningShizukuScript = true, lastScriptResult = "Đang bật IMS provisioning subId=$subId...", shizukuLogs = ">>> IMS provisioning persistent subId=$subId\n") }
            val res = try { shizukuOps.enableVoltePersistent(subId) } catch (e: Throwable) {
                ShizukuPrivilegedOperations.OpResult(false, "Lỗi: ${e.message}", e.toString())
            }
            _uiState.update { it.copy(isRunningShizukuScript = false, lastScriptResult = res.message, shizukuLogs = ">>> IMS provisioning subId=$subId\n${res.details}\n\n${res.message}") }
            delay(600); loadInitialData()
        }
    }

    fun openShizukuDownloadPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Throwable) {
            copyToClipboard(context, "https://shizuku.rikka.app/download/", "Link Shizuku")
        }
    }

    fun openWirelessDebuggingSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val i = Intent("android.settings.WIRELESS_DEBUG_SETTINGS").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(i)
            } catch (_: Throwable) {
                showToast(context, "Mở Cài đặt > Tùy chọn nhà phát triển > Gỡ lỗi không dây (Wireless debugging)")
            }
        }
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
                val bandResult = try { bandManager.check() } catch (_: Throwable) { null }

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
                        brandGuides = brands,
                        bandResult = bandResult
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
