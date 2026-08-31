package com.example.data.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

/**
 * Central manager for Shizuku / Sui binder lifecycle & privileged shell.
 * Dựa trên docs chính thức: https://github.com/RikkaApps/Shizuku-API
 * Phiên bản Shizuku API 13.1.5 (Maven Central 2025-2026)
 */
object ShizukuManager {

    private const val SHIZUKU_PERMISSION_CODE = 1001

    sealed class ShizukuState {
        data object NotInstalled : ShizukuState()
        data object NotRunning : ShizukuState()
        data object PermissionDenied : ShizukuState()
        data object ReadyShell : ShizukuState() // uid 2000 (ADB)
        data object ReadyRoot : ShizukuState()  // uid 0 (ROOT/Sui)
        data object Unknown : ShizukuState()
    }

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.Unknown)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var binderListener: Shizuku.OnBinderReceivedListener? = null
    private var binderDeadListener: Shizuku.OnBinderDeadListener? = null
    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    private var isInitialized = false

    /**
     * Must be called in Application.onCreate() or MainActivity.onCreate before any Shizuku call.
     * Tự động init Sui và đăng ký listener.
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        // Sui auto-initialized by ShizukuProvider since v12.1.0; no manual Sui.init needed
        // Bypass hidden API restriction on Android P+ (needed for ITelephony / ICarrierConfigLoader)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("L")
            }
        } catch (_: Throwable) {}

        binderListener = Shizuku.OnBinderReceivedListener {
            refreshStateAsync()
        }
        binderDeadListener = Shizuku.OnBinderDeadListener {
            _state.value = ShizukuState.NotRunning
        }
        permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) refreshStateAsync()
            else _state.value = ShizukuState.PermissionDenied
        }

        try {
            Shizuku.addBinderReceivedListenerSticky(binderListener!!)
            Shizuku.addBinderDeadListener(binderDeadListener!!)
            Shizuku.addRequestPermissionResultListener(permissionListener!!)
        } catch (_: Throwable) {}

        // Initial check after a short delay to allow provider to deliver binder
        refreshStateAsync()
    }

    fun destroy() {
        try { binderListener?.let { Shizuku.removeBinderReceivedListener(it) } } catch (_: Throwable) {}
        try { binderDeadListener?.let { Shizuku.removeBinderDeadListener(it) } } catch (_: Throwable) {}
        try { permissionListener?.let { Shizuku.removeRequestPermissionResultListener(it) } } catch (_: Throwable) {}
        isInitialized = false
    }

    fun refreshStateAsync() {
        _state.value = queryStateSync()
    }

    fun queryStateSync(): ShizukuState {
        return try {
            // pingBinder false nếu Shizuku chưa chạy / chưa cài
            val ping = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
            if (!ping) {
                // Phân biệt not installed vs not running bằng cách check packageManager
                return ShizukuState.NotRunning
            }
            val perm = try { Shizuku.checkSelfPermission() } catch (_: Throwable) { PackageManager.PERMISSION_DENIED }
            if (perm != PackageManager.PERMISSION_GRANTED) return ShizukuState.PermissionDenied

            val uid = try { Shizuku.getUid() } catch (_: Throwable) { 2000 }
            if (uid == 0) ShizukuState.ReadyRoot else ShizukuState.ReadyShell
        } catch (_: Throwable) {
            ShizukuState.NotRunning
        }
    }

    fun isReady(): Boolean = _state.value == ShizukuState.ReadyShell || _state.value == ShizukuState.ReadyRoot

    fun isPermissionGranted(): Boolean = try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (_: Throwable) { false }

    fun requestPermission(code: Int = SHIZUKU_PERMISSION_CODE) {
        try {
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                // User previously denied - still request again
            }
            Shizuku.requestPermission(code)
        } catch (_: Throwable) {}
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Throwable) {
            try {
                context.packageManager.getPackageInfo("rikka.shizuku", 0)
                true
            } catch (_: Throwable) { false }
        }
    }

    /**
     * Chạy một shell command với quyền Shizuku (uid 2000 hoặc 0).
     * Sử dụng Shizuku.newProcess - ưu tiên hơn Runtime.exec
     * @return ShellResult chứa stdout, stderr, exitCode
     */
    fun runShellCommand(command: String, timeoutMs: Long = 8000): ShellResult {
        if (!isReady()) return ShellResult("", "Shizuku not ready: ${_state.value}", -1)
        return try {
            // Shizuku.newProcess is private in 13.1.5 - use reflection
            val proc: Process? = try {
                val m = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                m.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                m.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
            } catch (_: Throwable) {
                // Fallback: try public hidden via transact?
                null
            }
            val process: Process = proc ?: return ShellResult("", "Failed to create Shizuku process (private API blocked)", -1)

            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val outThread = Thread {
                try { process.inputStream.bufferedReader().forEachLine { stdout.appendLine(it) } } catch (_: Throwable) {}
            }
            val errThread = Thread {
                try { process.errorStream.bufferedReader().forEachLine { stderr.appendLine(it) } } catch (_: Throwable) {}
            }
            outThread.start()
            errThread.start()
            val finished = if (timeoutMs > 0) {
                val deadline = System.currentTimeMillis() + timeoutMs
                var done = false
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val exit = process.exitValue()
                        done = true
                        break
                    } catch (_: IllegalThreadStateException) {
                        Thread.sleep(50)
                    }
                }
                if (!done) {
                    try { process.destroy() } catch (_: Throwable) {}
                    false
                } else true
            } else {
                process.waitFor()
                true
            }
            outThread.join(1000)
            errThread.join(1000)
            val exitCode = try { process.exitValue() } catch (_: Throwable) { if (finished) 0 else -1 }
            ShellResult(stdout.toString().trim(), stderr.toString().trim(), exitCode)
        } catch (e: Throwable) {
            ShellResult("", e.message ?: e.toString(), -1)
        }
    }

    /**
     * Chạy nhiều command nối tiếp, dừng nếu gặp lỗi? Không - chạy tất cả và gom log
     */
    fun runShellCommands(commands: List<String>): List<ShellResult> = commands.map { runShellCommand(it) }

    /**
     * Lấy system property qua `getprop` với Shizuku (nếu muốn bypass SELinux).
     */
    fun getSystemProperty(key: String): String? {
        val res = runShellCommand("getprop $key")
        return if (res.exitCode == 0 && res.stdout.isNotBlank()) res.stdout else null
    }

    fun setSystemProperty(key: String, value: String): ShellResult = runShellCommand("setprop $key $value")

    /**
     * Thử lấy system service binder qua ShizukuBinderWrapper.
     * Ví dụ: "carrier_config", "phone", "iphonesubinfo", "isub"
     * Dùng ServiceManager reflection + ShizukuBinderWrapper để có quyền ADB/ROOT.
     */
    fun getSystemServiceBinder(name: String): IBinder? {
        if (!isReady()) return null
        return try {
            val rawBinder: IBinder? = try {
                // Thử SystemServiceHelper nếu tồn tại (reflection để tránh compile error khi class thiếu)
                val helperClass = Class.forName("rikka.shizuku.SystemServiceHelper")
                val method = helperClass.getMethod("getSystemService", String::class.java)
                method.invoke(null, name) as? IBinder
            } catch (_: Throwable) {
                // Fallback: ServiceManager.getService via reflection
                val smClass = Class.forName("android.os.ServiceManager")
                val getService = smClass.getMethod("getService", String::class.java)
                getService.invoke(null, name) as? IBinder
            }
            if (rawBinder == null) return null
            ShizukuBinderWrapper(rawBinder)
        } catch (_: Throwable) { null }
    }

    fun isAdbUid(): Boolean = try { Shizuku.getUid() == 2000 } catch (_: Throwable) { false }
    fun isRootUid(): Boolean = try { Shizuku.getUid() == 0 } catch (_: Throwable) { false }

    data class ShellResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int
    ) {
        val isSuccess: Boolean get() = exitCode == 0
        val output: String get() = if (stdout.isNotBlank()) stdout else stderr
        override fun toString(): String = "exit=$exitCode out=$stdout err=$stderr"
    }
}
