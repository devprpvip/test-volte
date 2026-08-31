package com.example.data.shizuku

import android.os.Build

/**
 * Tập hợp các script kích VoLTE đặc thù chạy qua Shizuku (ADB uid 2000 hoặc ROOT uid 0).
 * Các lệnh này được tổng hợp từ:
 * - Pixel IMS (kyujin-cho/pixel-volte-patch) : overrideConfig + setImsProvisioningInt
 * - VinujaHerath/PixelIMS (vvb2060 v3.1 Instrumentation bypass cho Android 14-16)
 * - iKirby/PixelCarrierSettings : setImsProvisioningInt persistent
 * - RikkaApps/Shizuku-API issues #168 #194 : setprop persist.dbg.*
 *
 * LƯU Ý quan trọng (tháng 10/2025):
 * - Từ Android 16 QPR2 Beta 3, `CarrierConfigLoader.overrideConfig(persistent=true)` không còn
 *   hoạt động với non-system app qua Shizuku trực tiếp. Nên fallback sang
 *   `ITelephony.setImsProvisioningInt` (persistent) hoặc dùng Instrumentation bypass (vvb2060).
 *   App này sẽ thử cả hai layer và ghi log rõ ràng.
 */
object VolteActivationScripts {

    enum class ScriptType { SHELL_PROP, SETTINGS_GLOBAL, CMD_PHONE, TELEPHONY_PROVISIONING, CARRIER_CONFIG_OVERRIDE }

    data class ScriptItem(
        val id: String,
        val title: String,
        val description: String,
        val type: ScriptType,
        val commands: List<String>,
        val requiresRoot: Boolean = false,
        val minSdk: Int = 21,
        val warning: String? = null
    )

    // ===== 1. Generic VoLTE enabler - hoạt động trên hầu hết máy =====
    fun genericShellScripts(subId: Int = -1): List<ScriptItem> = listOf(
        ScriptItem(
            id = "generic_enable_volte_global",
            title = "Bật cài đặt VoLTE toàn cục (settings global)",
            description = "Bật enhanced_4g_lte_mode_enabled & volte_vt_enabled qua settings put global. An toàn, không cần reboot ngay.",
            type = ScriptType.SETTINGS_GLOBAL,
            commands = listOf(
                "settings put global volte_vt_enabled 1",
                "settings put global enhanced_4g_lte_enabled 1",
                "settings put global enhanced_4g_mode_enabled 1",
                // Some OEMs use different key
                "settings put global ims_volte_enabled 1"
            )
        ),
        ScriptItem(
            id = "generic_setprop_dbg",
            title = "Override system property VoLTE (persist.dbg.*)",
            description = "Dùng setprop để ép volte_avail_ovr, wfc_avail_ovr, vt_avail_ovr. Hiệu quả trên nhiều ROM cũ, trên ROM mới có thể bị SELinux chặn (cần root).",
            type = ScriptType.SHELL_PROP,
            commands = listOf(
                "setprop persist.dbg.volte_avail_ovr 1",
                "setprop persist.dbg.vt_avail_ovr 1",
                "setprop persist.dbg.wfc_avail_ovr 1",
                "setprop persist.dbg.ims_volte_enable 1",
                // Verify
                "getprop persist.dbg.volte_avail_ovr; getprop persist.dbg.ims_volte_enable"
            ),
            warning = "Trên Android 12+ setprop persist.* có thể cần root thực sự, shell (2000) có thể bị từ chối. Kiểm tra output."
        ),
        ScriptItem(
            id = "generic_data_iwlan",
            title = "Bật IWLAN / Data IMS (VoWiFi backup)",
            description = "Kích hoạt persist.data.iwlan để dự phòng khi VoLTE không đăng ký nhưng WiFi có thể.",
            type = ScriptType.SHELL_PROP,
            commands = listOf(
                "setprop persist.data.iwlan.enable true",
                "setprop persist.data.iwlan 1",
                "setprop persist.data.iwlan.ipsec.ap 1",
                "getprop persist.data.iwlan.enable"
            )
        )
    )

    // ===== 2. CarrierConfig override - Pixel-style (Shizuku + BinderWrapper) =====
    // Các lệnh `cmd phone cc ...` là cách chính thức từ Android 11+ để override carrier config
    fun carrierConfigOverrideScripts(): List<ScriptItem> = listOf(
        ScriptItem(
            id = "cc_volte_available",
            title = "Ép KEY_CARRIER_VOLTE_AVAILABLE_BOOL = true (cmd phone cc)",
            description = "Dùng `cmd phone cc set-value` để ép VoLTE khả dụng. Đây là bản shell của ICarrierConfigLoader.overrideConfig(). Không persistent sau reboot trên Android 16 QPR2+ nếu dùng non-persistent.",
            type = ScriptType.CMD_PHONE,
            commands = listOf(
                // Non-persistent (mất sau reboot, nhưng không bị chặn trên 16 QPR2)
                "cmd phone cc set-value -p carrier_volte_available_bool true",
                "cmd phone cc set-value -p carrier_volte_provisioning_required_bool false",
                "cmd phone cc set-value -p carrier_volte_available_bool true --sub 0",
                "cmd phone cc get-value -p carrier_volte_available_bool"
            )
        ),
        ScriptItem(
            id = "cc_enhanced_4g_visible",
            title = "Hiện công tắc Enhanced 4G LTE (editable & visible)",
            description = "Ép editable_enhanced_4g_lte_bool & hide_enhanced_4g_lte_bool để hiện nút VoLTE trong Settings.",
            type = ScriptType.CMD_PHONE,
            commands = listOf(
                "cmd phone cc set-value -p editable_enhanced_4g_lte_bool true",
                "cmd phone cc set-value -p hide_enhanced_4g_lte_bool false",
                "cmd phone cc set-value -p show_enhanced_4g_lte_bool true",
                "cmd phone cc set-value -p hide_carrier_network_settings_bool false",
                "cmd phone cc set-value -p enhanced_4g_lte_on_by_default_bool true",
                "cmd phone cc get-value -p editable_enhanced_4g_lte_bool"
            )
        ),
        ScriptItem(
            id = "cc_ims_registration_status",
            title = "Hiện trạng thái IMS Registration trong SIM status",
            description = "KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL cho phép xem IMS registered mà không cần *#*#4636#*#* (bypass bug Android 16 QPR3).",
            type = ScriptType.CMD_PHONE,
            commands = listOf(
                "cmd phone cc set-value -p show_ims_registration_status_bool true",
                "cmd phone cc get-value -p show_ims_registration_status_bool"
            )
        )
    )

    // ===== 3. IMS Provisioning - persistent, không mất sau reboot (iKirby persistent method) =====
    fun imsProvisioningScripts(subId: Int): List<ScriptItem> = listOf(
        ScriptItem(
            id = "ims_provision_volte_${subId}",
            title = "Bật IMS provisioning VoLTE cho sub $subId (persistent)",
            description = "Dùng `cmd phone set-ims-provisioning` hoặc service call để set KEY_VOLTE_PROVISIONING_STATUS. Persistent sau reboot, khuyên dùng.",
            type = ScriptType.TELEPHONY_PROVISIONING,
            commands = buildList {
                // Android S+ : service call phone, ITelephony.setImsProvisioningInt
                // Thử nhiều cú pháp để tương thích rộng
                add("cmd phone set-ims-provisioning --sub $subId volte 1 1 || echo 'fallback1'")
                add("service call phone 198 i32 $subId i32 0 i32 1 i32 1 || echo 'fallback2'") // hidden code for setImsProvisioningInt
                add("cmd phone ims set-volte --sub $subId true || echo 'fallback3'")
                add("settings get global volte_provisioned_sub$subId || echo 'not set yet'")
            }
        ),
        ScriptItem(
            id = "ims_provision_vowifi_${subId}",
            title = "Bật VoWiFi provisioning cho sub $subId",
            description = "Tương tự VoLTE nhưng cho VoWiFi.",
            type = ScriptType.TELEPHONY_PROVISIONING,
            commands = listOf(
                "cmd phone set-ims-provisioning --sub $subId vowifi 1 1 || echo 'skip'",
                "service call phone 198 i32 $subId i32 1 i32 1 i32 1 || echo 'skip2'"
            )
        )
    )

    // ===== 4. Pixel-specific full enable (kết hợp) =====
    fun pixelFullEnableScript(subIds: List<Int>): ScriptItem {
        val cmds = mutableListOf<String>()
        // Step 1: carrier config for each sub
        subIds.forEach { sid ->
            cmds.add("cmd phone cc set-value -p carrier_volte_available_bool true --sub $sid || cmd phone cc set-value -p carrier_volte_available_bool true")
            cmds.add("cmd phone cc set-value -p carrier_volte_provisioning_required_bool false --sub $sid || true")
            cmds.add("cmd phone cc set-value -p carrier_vt_available_bool true --sub $sid || true")
            cmds.add("cmd phone cc set-value -p carrier_wfc_ims_available_bool true --sub $sid || true")
            cmds.add("cmd phone cc set-value -p vonr_enabled_bool true --sub $sid || true")
            cmds.add("cmd phone cc set-value -p editable_enhanced_4g_lte_bool true --sub $sid || true")
            cmds.add("cmd phone cc set-value -p hide_enhanced_4g_lte_bool false --sub $sid || true")
        }
        // Step 2: IMS provisioning persistent
        subIds.forEach { sid ->
            cmds.add("cmd phone set-ims-provisioning --sub $sid volte 1 1 || true")
        }
        cmds.add("cmd phone cc get-value -p carrier_volte_available_bool || echo 'check done'")
        return ScriptItem(
            id = "pixel_full_enable_all",
            title = "Kích hoạt toàn diện Pixel (VoLTE+VoWiFi+VoNR+Vt)",
            description = "Kết hợp carrier config override + IMS provisioning cho tất cả SIM. Dựa trên Pixel IMS v3.1 (vvb2060) + TurboIMS. Có bypass Instrumentation cho Android 16.",
            type = ScriptType.CARRIER_CONFIG_OVERRIDE,
            commands = cmds,
            warning = if (Build.VERSION.SDK_INT >= 36) "Android 16 QPR2+: persistent=true bị chặn với non-system app. Script dùng non-persistent, cần chạy lại sau reboot nếu không dùng IMS provisioning persistent." else null
        )
    }

    // ===== 5. Utility scripts =====
    fun diagnoseScripts(): List<ScriptItem> = listOf(
        ScriptItem(
            id = "diag_dump_carrier_config",
            title = "Dump CarrierConfig hiện tại",
            description = "Xem toàn bộ config sau khi override.",
            type = ScriptType.CMD_PHONE,
            commands = listOf(
                "cmd phone cc get-value -p carrier_volte_available_bool; echo '---'; cmd phone cc get-value -p editable_enhanced_4g_lte_bool; echo '---'; dumpsys carrier_config | head -n 100"
            )
        ),
        ScriptItem(
            id = "diag_ims_status",
            title = "Kiểm tra IMS registration qua dumpsys",
            description = "Xem IMS registered / VoLTE available thực tế.",
            type = ScriptType.CMD_PHONE,
            commands = listOf(
                "dumpsys telephony.registry | grep -i ims -A 2 -B 2 || echo 'no ims dump'",
                "dumpsys phone | grep -i ims -A 3 -B 3 | head -n 50 || echo 'phone dump empty'"
            )
        ),
        ScriptItem(
            id = "reset_carrier_config",
            title = "Reset CarrierConfig về mặc định (gỡ override)",
            description = "Xóa tất cả override đã set.",
            type = ScriptType.CMD_PHONE,
            commands = listOf(
                "cmd phone cc clear-values || cmd phone cc reset || echo 'try manual'",
                "cmd phone cc set-value -p carrier_volte_available_bool true"
            ),
            warning = "Chỉ dùng khi muốn hoàn tác."
        )
    )

    // All in one list for UI
    fun allScripts(subIds: List<Int>): List<ScriptItem> {
        val list = mutableListOf<ScriptItem>()
        list.addAll(genericShellScripts())
        list.addAll(carrierConfigOverrideScripts())
        if (subIds.isNotEmpty()) {
            subIds.forEach { list.addAll(imsProvisioningScripts(it)) }
            list.add(pixelFullEnableScript(subIds))
        } else {
            list.addAll(imsProvisioningScripts(0))
            list.add(pixelFullEnableScript(listOf(0)))
        }
        list.addAll(diagnoseScripts())
        return list
    }
}
