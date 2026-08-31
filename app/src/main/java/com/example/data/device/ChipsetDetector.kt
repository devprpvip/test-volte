package com.example.data.device

import android.os.Build
import android.util.Log

/**
 * Phát hiện loại chipset để phân luồng fix VoLTE.
 * Luồng yêu cầu:
 *  - MTK (MediaTek)  → tự động gọi Engineering Mode để bật VoLTE
 *  - QUALCOMM (Snapdragon) → cảnh báo EFS → thử menu ẩn → Shizuku
 *
 * Dựa trên Build.* + SystemProperties (ro.*) + /proc/cpuinfo heuristic.
 */
object ChipsetDetector {

    enum class ChipsetType {
        MEDIATEK,
        QUALCOMM,
        EXYNOS,
        UNISOC,
        KIRIN,
        TENSOR,
        UNKNOWN
    }

    data class ChipsetInfo(
        val type: ChipsetType,
        val label: String,              // "MediaTek" / "Qualcomm Snapdragon" ...
        val shortLabel: String,         // "MTK" / "Qualcomm"
        val socModel: String = "",
        val socManufacturer: String = "",
        val hardware: String = Build.HARDWARE ?: "",
        val board: String = Build.BOARD ?: "",
        val platform: String = "",
        val rawDump: String = ""
    )

    fun detect(): ChipsetInfo {
        val hardware = (Build.HARDWARE ?: "").lowercase()
        val board = (Build.BOARD ?: "").lowercase()
        val socModel = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL ?: "" else ""
        } catch (_: Throwable) { "" }
        val socMan = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER ?: "" else ""
        } catch (_: Throwable) { "" }

        val props = readSystemProperties()

        val dump = buildString {
            append("HARDWARE=$hardware; BOARD=$board; SOC_MODEL=$socModel; SOC_MAN=$socMan; ")
            props.forEach { (k, v) -> if (v.isNotBlank()) append("$k=$v; ") }
        }

        val combined = (hardware + " " + board + " " + socModel.lowercase() + " " + socMan.lowercase() + " " +
                props.values.joinToString(" ").lowercase() + " " + readCpuInfoSnippet().lowercase()
                ).lowercase()

        val type = when {
            containsAny(combined, listOf("mt", "mediatek", "helio", "dimensity", "mt68", "mt67", "mt69", "mt87", "mt86", "mt65", "k698", "k683")) -> ChipsetType.MEDIATEK
            containsAny(combined, listOf("qcom", "qualcomm", "snapdragon", "sm8", "sm6", "sdm", "msm", "apq", "qct")) -> ChipsetType.QUALCOMM
            containsAny(combined, listOf("exynos", "s5e", "universal9", "universal8")) -> ChipsetType.EXYNOS
            containsAny(combined, listOf("unisoc", "ums", "sc98", "t610", "t612", "t606", "ums9230")) -> ChipsetType.UNISOC
            containsAny(combined, listOf("kirin", "hi36", "hi62")) -> ChipsetType.KIRIN
            containsAny(combined, listOf("tensor", "gs101", "gs201", "zuma")) -> ChipsetType.TENSOR
            else -> {
                // Fallback strict hardware checks
                when {
                    hardware.startsWith("mt") -> ChipsetType.MEDIATEK
                    hardware.contains("qcom") || hardware.contains("sm") -> ChipsetType.QUALCOMM
                    else -> ChipsetType.UNKNOWN
                }
            }
        }

        val (label, short) = when (type) {
            ChipsetType.MEDIATEK -> "MediaTek" to "MTK"
            ChipsetType.QUALCOMM -> "Qualcomm Snapdragon" to "Qualcomm"
            ChipsetType.EXYNOS -> "Samsung Exynos" to "Exynos"
            ChipsetType.UNISOC -> "Unisoc" to "Unisoc"
            ChipsetType.KIRIN -> "HiSilicon Kirin" to "Kirin"
            ChipsetType.TENSOR -> "Google Tensor" to "Tensor"
            ChipsetType.UNKNOWN -> "Không xác định" to "Unknown"
        }

        val platform = props["ro.board.platform"] ?: props["ro.mediatek.platform"] ?: props["ro.hardware"] ?: ""

        return ChipsetInfo(
            type = type,
            label = label,
            shortLabel = short,
            socModel = socModel,
            socManufacturer = socMan,
            hardware = Build.HARDWARE ?: "",
            board = Build.BOARD ?: "",
            platform = platform,
            rawDump = dump
        )
    }

    fun isMtk(info: ChipsetInfo = detect()): Boolean = info.type == ChipsetType.MEDIATEK
    fun isQualcomm(info: ChipsetInfo = detect()): Boolean = info.type == ChipsetType.QUALCOMM

    fun displayLabel(info: ChipsetInfo): String {
        val soc = info.socModel.takeIf { it.isNotBlank() } ?: info.hardware
        return when (info.type) {
            ChipsetType.MEDIATEK -> if (soc.isNotBlank() && soc.lowercase() != "mtk") "MTK • $soc" else "MTK • MediaTek"
            ChipsetType.QUALCOMM -> if (soc.isNotBlank()) "Qualcomm • $soc" else "Qualcomm Snapdragon"
            ChipsetType.EXYNOS -> "Exynos • $soc"
            ChipsetType.UNISOC -> "Unisoc • $soc"
            ChipsetType.KIRIN -> "Kirin • $soc"
            ChipsetType.TENSOR -> "Tensor • $soc"
            ChipsetType.UNKNOWN -> soc.ifBlank { "Không rõ" }
        }
    }

    private fun containsAny(haystack: String, needles: List<String>): Boolean =
        needles.any { haystack.contains(it) }

    private fun readSystemProperties(): Map<String, String> {
        val keys = listOf(
            "ro.board.platform",
            "ro.hardware",
            "ro.hardware.chipname",
            "ro.mediatek.platform",
            "ro.mediatek.version.release",
            "ro.vendor.mediatek.platform",
            "ro.soc.model",
            "ro.soc.manufacturer",
            "ro.boot.hardware",
            "ro.product.board",
            "ro.chipname",
            "ro.vendor.product.cpu.abilist",
            "ro.soc.manufacturer",
            "ro.odm.product.cpu.model"
        )
        val map = mutableMapOf<String, String>()
        for (k in keys) {
            val v = getSystemProperty(k) ?: ""
            if (v.isNotBlank()) map[k] = v
        }
        return map
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val m = c.getMethod("get", String::class.java, String::class.java)
            m.invoke(null, key, "") as? String
        } catch (_: Throwable) { null }
    }

    private fun readCpuInfoSnippet(): String {
        return try {
            val f = java.io.File("/proc/cpuinfo")
            if (!f.exists()) return ""
            val txt = f.readText()
            // Only first 2k chars to avoid big log
            if (txt.length > 2048) txt.substring(0, 2048) else txt
        } catch (_: Throwable) { "" }
    }
}
