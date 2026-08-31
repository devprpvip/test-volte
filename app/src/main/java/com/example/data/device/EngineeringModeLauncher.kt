package com.example.data.device

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Mở Engineering Mode cho chip MediaTek (MTK).
 * Thử nhiều component ẩn khác nhau; nếu không mở được thì fallback về dial code *#*#3646633#*#*.
 */
object EngineeringModeLauncher {

    private const val TAG = "EngModeLauncher"

    data class LaunchResult(val success: Boolean, val method: String, val error: String? = null)

    // Danh sách Intent ứng viên cho MTK EngineerMode
    private fun candidateIntents(): List<Intent> = listOf(
        // MTK EngineerMode chính thức
        Intent().setComponent(ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode")),
        Intent().setComponent(ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerModeActivity")),
        Intent().setComponent(ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.MTKLogger")),
        // Một số ROM custom
        Intent().setComponent(ComponentName("com.android.engineeringmode", "com.android.engineeringmode.EngineeringModeActivity")),
        Intent("com.mediatek.ENGINEERMODE"),
        Intent("android.intent.action.MAIN").setClassName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode"),
        // Telephony IMS entry (thử mở trực tiếp IMS settings)
        Intent().setComponent(ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.telephony.TelephonySettings")),
        Intent().setComponent(ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.ims.ImsSettings")),
    )

    fun openMtkEngineeringMode(context: Context): LaunchResult {
        for (intent in candidateIntents()) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Thử resolve trước để tránh crash không cần
                val can = try {
                    context.packageManager.resolveActivity(intent, 0) != null
                } catch (_: Throwable) { true } // nếu check lỗi, cứ thử launch
                if (!can) continue
                context.startActivity(intent)
                Log.i(TAG, "Opened EngineerMode via $intent")
                return LaunchResult(true, intent.component?.flattenToShortString() ?: intent.action ?: "unknown")
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "Not found: $intent -> ${e.message}")
            } catch (e: SecurityException) {
                Log.w(TAG, "Security: $intent -> ${e.message}")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed: $intent -> ${e.message}")
            }
        }
        // Fallback: dial code MTK
        return try {
            dialCode(context, "*#*#3646633#*#*")
            LaunchResult(true, "dial:*#*#3646633#*#*")
        } catch (e: Throwable) {
            LaunchResult(false, "dial_failed", e.message)
        }
    }

    /**
     * Mở IMS-specific trong EngineerMode nếu có (telephony -> IMS).
     * Chỉ dùng khi đã vào được EngineerMode.
     */
    fun openMtkImsSettings(context: Context): LaunchResult {
        val imsIntents = listOf(
            Intent().setComponent(ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.telephony.TelephonySettings")),
            Intent().setComponent(ComponentName("com.mediatek.engineermode", "com.mediatek.engineermode.ims.ImsSettings")),
            Intent("com.mediatek.engineermode.IMS_SETTINGS"),
        )
        for (i in imsIntents) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                return LaunchResult(true, i.component?.flattenToShortString() ?: "ims_intent")
            } catch (_: Throwable) {}
        }
        return LaunchResult(false, "no_ims_entry")
    }

    private fun dialCode(context: Context, code: String) {
        val encoded = Uri.encode("#")
        val formatted = code.replace("#", encoded)
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$formatted")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
