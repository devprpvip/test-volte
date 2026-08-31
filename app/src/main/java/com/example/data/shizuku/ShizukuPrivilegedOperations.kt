package com.example.data.shizuku

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Các thao tác đặc quyền qua Shizuku để kích VoLTE.
 * Có 3 tầng:
 * 1) Shell `cmd phone cc` / `settings put` / `setprop`  => qua ShizukuManager.runShellCommand
 * 2) Binder trực tiếp ICarrierConfigLoader.overrideConfig() => qua ShizukuBinderWrapper + reflection
 * 3) ITelephony.setImsProvisioningInt() => persistent sau reboot (iKirby persistent method)
 *
 * Tất cả đều yêu cầu Shizuku READY và đã grant permission.
 * Tham khảo: Pixel IMS (vvb2060 Instrumentation bypass) & Shizuku API README.
 */
class ShizukuPrivilegedOperations(private val context: Context) {

    private val TAG = "ShizukuPrivilegedOp"

    data class OpResult(
        val success: Boolean,
        val message: String,
        val details: String = "",
        val warning: String? = null
    )

    suspend fun runScript(script: VolteActivationScripts.ScriptItem): OpResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isReady()) {
            return@withContext OpResult(false, "Shizuku chưa sẵn sàng (${ShizukuManager.state.value}). Hãy cài Shizuku và cấp quyền.", "")
        }
        val logs = mutableListOf<String>()
        var allSuccess = true
        for (cmd in script.commands) {
            val res = ShizukuManager.runShellCommand(cmd)
            logs.add("$ $cmd\n→ exit=${res.exitCode} out=${res.stdout.ifBlank { res.stderr }}")
            // Một số lệnh fallback có `|| echo 'fallback'` nên exit 0 nhưng chứa fallback tag
            if (res.exitCode != 0 && !cmd.contains("||")) {
                // Không fail ngay, chỉ đánh dấu
                // Đặc biệt setprop có thể bị permission denied trên shell (chỉ root mới được) -> cảnh báo nhưng vẫn tiếp tục
                if (res.stderr.contains("permission", ignoreCase = true) || res.stderr.contains("denied", ignoreCase = true)) {
                    logs.add("⚠ Permission denied cho lệnh này (cần ROOT, hiện đang Shell uid=2000). Thử lệnh khác.")
                }
                allSuccess = false
            }
        }
        val detail = logs.joinToString("\n\n")
        OpResult(
            success = allSuccess || detail.contains("true", ignoreCase = true) || detail.contains("1"),
            message = if (allSuccess) "Đã chạy xong script: ${script.title}" else "Chạy script với một số cảnh báo: ${script.title}",
            details = detail,
            warning = script.warning
        )
    }

    /**
     * Thử override CarrierConfig trực tiếp qua binder ICarrierConfigLoader.
     * Đây là phương thức gốc mà Pixel IMS dùng: ICarrierConfigLoader.overrideConfig(subId, bundle, persistent).
     * Cần hidden API bypass + ShizukuBinderWrapper.
     * Trả về false nếu bị patch chặn (Android 16 QPR2+) hoặc binder không khả dụng.
     */
    suspend fun overrideCarrierConfigViaBinder(
        subId: Int,
        persistent: Boolean = false
    ): OpResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isReady()) return@withContext OpResult(false, "Shizuku chưa sẵn sàng", "")
        try {
            val wrapper = ShizukuManager.getSystemServiceBinder("carrier_config")
                ?: return@withContext OpResult(false, "Không lấy được binder carrier_config", "")
            // Lấy ICarrierConfigLoader.Stub.asInterface
            val iCarrierConfigLoaderClass = Class.forName("android.telephony.ICarrierConfigLoader")
            val stubClass = Class.forName("android.telephony.ICarrierConfigLoader\$Stub")
            val asInterface = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val loader = asInterface.invoke(null, wrapper)

            // Tạo bundle override: carrier_volte_available_bool = true, v.v.
            val bundle = android.os.PersistableBundle().apply {
                putBoolean("carrier_volte_available_bool", true)
                putBoolean("carrier_volte_provisioning_required_bool", false)
                putBoolean("carrier_vt_available_bool", true)
                putBoolean("carrier_wfc_ims_available_bool", true)
                putBoolean("vonr_enabled_bool", true)
                putBoolean("vonr_setting_visibility_bool", true)
                putBoolean("editable_enhanced_4g_lte_bool", true)
                putBoolean("hide_enhanced_4g_lte_bool", false)
                putBoolean("show_enhanced_4g_lte_bool", true)
                putBoolean("hide_carrier_network_settings_bool", false)
                putBoolean("enhanced_4g_lte_on_by_default_bool", true)
                putBoolean("editable_wfc_mode_bool", true)
                putBoolean("editable_wfc_roaming_mode_bool", true)
                putBoolean("show_ims_registration_status_bool", true)
                // Version stamp để app check canPersistent
                putInt("vvb2060_config_version", 31)
            }

            // Method: overrideConfig(int subId, PersistableBundle bundle, boolean persistent)
            // Trên Android S+ có thêm param persistent? Check signature
            val methods = iCarrierConfigLoaderClass.methods.filter { it.name == "overrideConfig" }
            val method = methods.firstOrNull { it.parameterTypes.size == 3 }
                ?: methods.firstOrNull()
                ?: return@withContext OpResult(false, "Không tìm thấy method overrideConfig", "")

            try {
                if (method.parameterTypes.size == 3) {
                    method.invoke(loader, subId, bundle, persistent)
                } else {
                    method.invoke(loader, subId, bundle)
                }
            } catch (e: java.lang.reflect.InvocationTargetException) {
                val cause = e.cause ?: e
                val msg = cause.message ?: cause.toString()
                // Nếu bị SecurityException do patch Oct 2025
                if (msg.contains("SecurityException", ignoreCase = true) || msg.contains("permission", ignoreCase = true)) {
                    return@withContext OpResult(
                        false,
                        "Bị chặn bởi patch bảo mật Oct 2025 (CVE-2025-48617). Cần dùng Instrumentation bypass (PixelIMS v3.1) hoặc fallback sang IMS provisioning persistent.",
                        msg,
                        warning = "Trên Android 16 QPR2+ persistent=true bị chặn. Hãy dùng non-persistent + IMS provisioning."
                    )
                }
                throw cause
            }

            OpResult(true, "Đã override CarrierConfig cho subId=$subId (persistent=$persistent) qua binder trực tiếp.", "bundle keys: ${bundle.keySet()?.joinToString()}")

        } catch (e: Throwable) {
            Log.e(TAG, "overrideCarrierConfigViaBinder failed", e)
            OpResult(false, "Lỗi binder override: ${e.message}", Log.getStackTraceString(e))
        }
    }

    /**
     * Persistent IMS provisioning qua ITelephony.setImsProvisioningInt
     * KEY: 0=VOLTE, 1=VT, 2=VoWiFi, etc. Dùng hidden API.
     * Đây là cách iKirby làm để persistent sau reboot (không bị reset như overrideConfig).
     */
    suspend fun setImsProvisioningViaBinder(
        subId: Int,
        key: Int = 0, // 0 = VOLTE
        value: Int = 1
    ): OpResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isReady()) return@withContext OpResult(false, "Shizuku chưa sẵn sàng", "")
        try {
            val wrapper = ShizukuManager.getSystemServiceBinder("phone")
                ?: return@withContext OpResult(false, "Không lấy được binder phone", "")
            val telephonyClass = Class.forName("com.android.internal.telephony.ITelephony")
            val stubClass = Class.forName("com.android.internal.telephony.ITelephony\$Stub")
            val asInterface = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val telephony = asInterface.invoke(null, wrapper)

            // Try: setImsProvisioningInt(int subId, int key, int value)  // hidden transaction 198
            val method = try {
                telephonyClass.getMethod("setImsProvisioningInt", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            } catch (_: Throwable) {
                telephonyClass.methods.firstOrNull { it.name == "setImsProvisioningInt" }
            } ?: return@withContext OpResult(false, "Không tìm thấy setImsProvisioningInt", "")

            try {
                method.invoke(telephony, subId, key, value)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw e.cause ?: e
            }
            OpResult(true, "Đã set IMS provisioning subId=$subId key=$key value=$value (persistent).", "")
        } catch (e: Throwable) {
            Log.e(TAG, "setImsProvisioningViaBinder failed", e)
            // Fallback: thử shell cmd phone
            val shellRes = ShizukuManager.runShellCommand("cmd phone set-ims-provisioning --sub $subId volte 1 1 || service call phone 198 i32 $subId i32 0 i32 1 i32 1")
            OpResult(
                shellRes.isSuccess || shellRes.stdout.contains("1"),
                if (shellRes.isSuccess) "Đã set IMS provisioning qua shell fallback" else "Lỗi cả binder và shell: ${e.message}",
                "binder error: ${e.message}\nshell: $shellRes"
            )
        }
    }

    suspend fun enableVoltePersistent(subId: Int): OpResult = setImsProvisioningViaBinder(subId, 0, 1)

    /**
     * Bật AdvancedCalling (MmTel) qua ImsMmTelManager.setAdvancedCallingSettingEnabled
     * Cần MODIFY_PHONE_STATE - qua Shizuku có thể gọi qua shell `cmd phone` hoặc binder.
     */
    suspend fun setAdvancedCallingEnabled(subId: Int, enabled: Boolean): OpResult = withContext(Dispatchers.IO) {
        // Thử shell trước (đơn giản nhất)
        val res = ShizukuManager.runShellCommand(
            "cmd phone set-advanced-calling --sub $subId ${if (enabled) "true" else "false"} || settings put global volte_vt_enabled ${if (enabled) 1 else 0}"
        )
        OpResult(res.isSuccess, if (res.isSuccess) "Đã set AdvancedCalling=$enabled cho sub $subId" else "Không set được AdvancedCalling: ${res.stderr}", res.toString())
    }

    /**
     * Kiểm tra xem override có persistent được không (trên Android 16 QPR2+ thì false).
     * Logic từ PixelIMS v3.1: thử canPersistent()
     */
    suspend fun canPersistent(): Boolean = withContext(Dispatchers.IO) {
        // heuristic: nếu SDK >= 36 (Android 16) và patch đã apply thì likely false
        if (Build.VERSION.SDK_INT < 36) return@withContext true
        // Thử gọi method canPersistent qua binder nếu có
        try {
            val wrapper = ShizukuManager.getSystemServiceBinder("carrier_config") ?: return@withContext false
            val loaderClass = Class.forName("android.telephony.ICarrierConfigLoader")
            val stubClass = Class.forName("android.telephony.ICarrierConfigLoader\$Stub")
            val asInterface = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
            val loader = asInterface.invoke(null, wrapper)
            val m = loaderClass.methods.firstOrNull { it.name == "canPersistOverrideConfig" || it.name == "canOverrideConfigPersistently" }
            if (m != null) {
                val r = m.invoke(loader) as? Boolean
                return@withContext r ?: false
            }
        } catch (_: Throwable) {}
        return@withContext false // conservative
    }

    fun getActiveSubIds(): List<Int> {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val list = sm?.activeSubscriptionInfoList ?: return emptyList()
            list.map { it.subscriptionId }
        } catch (_: Throwable) { emptyList() }
    }
}
