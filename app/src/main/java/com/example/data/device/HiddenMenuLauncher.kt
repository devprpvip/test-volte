package com.example.data.device

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Mở các menu ẩn cho Qualcomm & các hãng khác trước khi fallback Shizuku.
 * Thứ tự theo flow: *#800# (Oppo/OnePlus) → *#*#4636#*#* (RadioInfo) → *#*#86583#*#* (Xiaomi) → *#0011# (Samsung) → *#*#3646633#*#* (MTK)
 */
object HiddenMenuLauncher {

    data class HiddenCode(
        val code: String,
        val label: String,
        val target: String,
        val description: String
    )

    val QUALCOMM_SEQUENCE = listOf(
        HiddenCode("*#800#", "Engineer Mode (Oppo/OnePlus)", "Oppo/Realme/OnePlus", "Mở Feedback/EngineerMode – nơi có IMS settings trên ColorOS/OxygenOS"),
        HiddenCode("*#*#4636#*#*", "Testing / Radio Info", "Universal", "Menu kiểm tra Radio & Phone Information – bật VoLTE Provisioned"),
        HiddenCode("*#*#86583#*#*", "Hiện VoLTE (Xiaomi)", "Xiaomi", "Tắt carrier check để hiện nút VoLTE (dù không phải Xiaomi vẫn thử safe)"),
        HiddenCode("*#0011#", "ServiceMode", "Samsung", "Kiểm tra IMS Registration & band trên Samsung"),
        HiddenCode("*#*#3646633#*#*", "MTK EngineerMode", "MediaTek", "Dự phòng nếu phát hiện MTK lẫn"),
    )

    /**
     * Thử mở trực tiếp RadioInfo activity qua intent (không cần dial).
     * Nếu không được thì fallback dial code.
     */
    fun tryOpenRadioInfoDirect(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.RadioInfo")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TestingSettings")),
            Intent("android.intent.action.MAIN").setClassName("com.android.settings", "com.android.settings.RadioInfo"),
            Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (context.packageManager.resolveActivity(intent, 0) == null) continue
                context.startActivity(intent)
                return true
            } catch (_: Throwable) {}
        }
        return false
    }

    fun dialCode(context: Context, code: String): Boolean {
        return try {
            val encoded = Uri.encode("#")
            val formatted = code.replace("#", encoded)
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$formatted")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Throwable) {
            Log.w("HiddenMenuLauncher", "dial $code failed ${e.message}")
            false
        }
    }

    /**
     * Mở code theo index trong QUALCOMM_SEQUENCE.
     * Trả về true nếu đã gửi intent dial.
     */
    fun openByIndex(context: Context, index: Int): Boolean {
        if (index !in QUALCOMM_SEQUENCE.indices) return false
        val item = QUALCOMM_SEQUENCE[index]
        // Ưu tiên RadioInfo direct cho mã 4636
        if (item.code == "*#*#4636#*#*") {
            if (tryOpenRadioInfoDirect(context)) return true
        }
        return dialCode(context, item.code)
    }

    fun openAllSequentially(context: Context): Boolean {
        // Thử cái đầu tiên trước, UI sẽ cho user confirm
        return openByIndex(context, 0)
    }
}
