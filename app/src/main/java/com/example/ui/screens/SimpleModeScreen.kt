package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.ChipsetDetector
import com.example.data.device.HiddenMenuLauncher
import com.example.data.model.ActiveStatus
import com.example.data.model.SupportStatus
import com.example.data.model.VisibilityStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VoLtePrimary
import com.example.ui.viewmodel.SimpleFixStage
import com.example.ui.viewmodel.VolteCheckerViewModel

@Composable
fun SimpleModeScreen(
    viewModel: VolteCheckerViewModel,
    onSwitchToClassic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onPermissionResult(granted) }

    // EFS Warning Dialog for Qualcomm
    if (uiState.showEfsWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEfsWarning() },
            icon = { Icon(Icons.Default.Warning, null, tint = StatusWarning, modifier = Modifier.size(32.dp)) },
            title = { Text("Cảnh báo phân vùng EFS — Qualcomm", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Thiết bị của bạn dùng chip Qualcomm Snapdragon. Phân vùng EFS chứa IMEI và cấu hình modem.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(shape = RoundedCornerShape(12.dp), color = StatusWarning.copy(alpha = 0.12f), border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f))) {
                        Text(
                            "⚠ Tuyệt đối KHÔNG can thiệp trực tiếp vào EFS qua setprop / QPST nếu không có backup. App này CHỈ thử mở menu ẩn hệ thống (*#800#, *#*#4636#*#*...) và sau đó dùng Shizuku + Pixel IMS (không chạm EFS thô).",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text("Bạn có muốn tiếp tục thử mở menu ẩn không? Nếu menu ẩn không hiện nút VoLTE, app sẽ tự chuyển sang Shizuku (an toàn).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmEfsWarning(context) }, colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary)) {
                    Text("Đã hiểu, tiếp tục →")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEfsWarning() }) { Text("Hủy") }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: App title + Lite badge + switch
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = VoLtePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Text("HD", fontWeight = FontWeight.ExtraBold, color = VoLtePrimary, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("VoLTE Checker", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.2f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))) {
                                Text("LITE 1.0.4", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Text("Bản siêu đơn giản • Tự động nhận diện chipset", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 14.sp)
                        Text("Fork riêng, không thay thế bản gốc 1.0.4", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                    IconButton(onClick = onSwitchToClassic) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Auto chipset banner
        item {
            val chip = uiState.chipsetInfo
            val isMtk = chip.type == ChipsetDetector.ChipsetType.MEDIATEK
            val isQc = chip.type == ChipsetDetector.ChipsetType.QUALCOMM
            val chipColor = when {
                isMtk -> StatusSuccess
                isQc -> VoLtePrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val chipBg = when {
                isMtk -> StatusSuccess.copy(alpha = 0.12f)
                isQc -> VoLtePrimary.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = chipBg), border = BorderStroke(1.dp, chipColor.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Memory, null, tint = chipColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động nhận diện chipset", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = chipColor)
                        Text(
                            when {
                                isMtk -> "MediaTek (MTK) — sẽ tự mở EngineerMode"
                                isQc -> "Qualcomm Snapdragon — sẽ cảnh báo EFS → thử menu ẩn"
                                else -> "${chip.label} — thử menu ẩn → Shizuku"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Phát hiện: ${uiState.cpuLabel} • ${chip.hardware.ifBlank { chip.board }} • ${chip.socModel.ifBlank { "—" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = chipColor)
                    } else {
                        Icon(
                            imageVector = if (isMtk || isQc) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = chipColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Permission banner if needed
        if (!uiState.hasPermission) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)), border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = StatusWarning, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cấp quyền Điện thoại để đọc SIM & IMS chính xác", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Không cấp quyền vẫn xem được, nhưng kết quả sẽ là giả định.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = StatusWarning), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Cấp quyền", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 5-item status table (yêu cầu: 1-2-3 giống bản gốc + 4 mã máy + 5 loại CPU)
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneInTalk, null, tint = VoLtePrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("BẢNG TRẠNG THÁI — 5 MỤC", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                    // 1 - Device supported
                    SimpleRow(
                        number = "1",
                        icon = when (uiState.verdict.deviceSupported) {
                            SupportStatus.SUPPORTED -> Icons.Default.CheckCircle
                            SupportStatus.NOT_SUPPORTED -> Icons.Default.Error
                            else -> Icons.Default.Warning
                        },
                        color = when (uiState.verdict.deviceSupported) {
                            SupportStatus.SUPPORTED -> StatusSuccess
                            SupportStatus.NOT_SUPPORTED -> StatusError
                            else -> StatusWarning
                        },
                        title = "Hỗ trợ VoLTE phần cứng",
                        value = when (uiState.verdict.deviceSupported) {
                            SupportStatus.SUPPORTED -> "Có — modem LTE & IMS sẵn sàng"
                            SupportStatus.NOT_SUPPORTED -> "Không hỗ trợ"
                            SupportStatus.PARTIALLY_SUPPORTED -> "Hỗ trợ một phần"
                            SupportStatus.UNKNOWN -> "Đang kiểm tra…"
                        },
                        subtitle = uiState.verdict.deviceSupportReason
                    )
                    // 2 - VoLTE enabled
                    SimpleRow(
                        number = "2",
                        icon = when (uiState.verdict.isVolteEnabled) {
                            ActiveStatus.ACTIVE_REGISTERED -> Icons.Default.PhoneInTalk
                            ActiveStatus.PROVISIONED_READY -> Icons.Default.CheckCircle
                            else -> Icons.Default.Warning
                        },
                        color = when (uiState.verdict.isVolteEnabled) {
                            ActiveStatus.ACTIVE_REGISTERED -> StatusSuccess
                            ActiveStatus.PROVISIONED_READY -> VoLtePrimary
                            else -> StatusWarning
                        },
                        title = "VoLTE đã bật chưa?",
                        value = when (uiState.verdict.isVolteEnabled) {
                            ActiveStatus.ACTIVE_REGISTERED -> "Đang hoạt động (IMS Registered) ✅"
                            ActiveStatus.PROVISIONED_READY -> "Đã cấp phép / Sẵn sàng"
                            ActiveStatus.DISABLED -> "Chưa bật / Chưa đăng ký"
                            ActiveStatus.NOT_PROVISIONED -> "Chưa cấp phép"
                            ActiveStatus.UNKNOWN -> "Đang kiểm tra…"
                        },
                        subtitle = uiState.verdict.enabledStatusReason
                    )
                    // 3 - Settings visibility
                    SimpleRow(
                        number = "3",
                        icon = if (uiState.verdict.settingsVisibility == VisibilityStatus.VISIBLE) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        color = if (uiState.verdict.settingsVisibility == VisibilityStatus.VISIBLE) StatusSuccess else StatusWarning,
                        title = "Nút VoLTE trong Cài đặt",
                        value = when (uiState.verdict.settingsVisibility) {
                            VisibilityStatus.VISIBLE -> "Đang hiện (không ẩn) ✅"
                            VisibilityStatus.HIDDEN_BY_OEM -> "BỊ ẨN do khóa hãng"
                            VisibilityStatus.HIDDEN_BY_CARRIER -> "BỊ ẨN bởi nhà mạng"
                            VisibilityStatus.LOCKED_RESTRICTED -> "Bị giới hạn vùng / cần mở khóa"
                            VisibilityStatus.UNKNOWN -> "Đang kiểm tra…"
                        },
                        subtitle = uiState.verdict.visibilityReason
                    )
                    // 4 - Device code
                    SimpleRow(
                        number = "4",
                        icon = Icons.Default.Smartphone,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = "Mã máy",
                        value = "${uiState.deviceInfo.manufacturer} ${uiState.deviceInfo.model} • ${uiState.deviceInfo.brand} • ${uiState.deviceInfo.device}",
                        subtitle = "Board: ${uiState.deviceInfo.board} • HW: ${uiState.deviceInfo.hardware} • ${uiState.deviceInfo.androidVersion}"
                    )
                    // 5 - CPU type
                    SimpleRow(
                        number = "5",
                        icon = Icons.Default.Memory,
                        color = when (uiState.chipsetInfo.type) {
                            ChipsetDetector.ChipsetType.MEDIATEK -> StatusSuccess
                            ChipsetDetector.ChipsetType.QUALCOMM -> VoLtePrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        title = "Loại CPU",
                        value = "${uiState.chipsetInfo.shortLabel} — ${uiState.cpuLabel}",
                        subtitle = "SoC: ${uiState.chipsetInfo.socModel.ifBlank { uiState.chipsetInfo.hardware.ifBlank { "—" }}} • Nhà SX SoC: ${uiState.chipsetInfo.socManufacturer.ifBlank { "—" }} • Platform: ${uiState.chipsetInfo.platform.ifBlank { "—" }}"
                    )
                }
            }
        }

        // Overall verdict banner
        item {
            val allGood = uiState.simpleIsAllGood
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (allGood) StatusSuccess.copy(alpha = 0.12f) else StatusWarning.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, if (allGood) StatusSuccess.copy(alpha = 0.35f) else StatusWarning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (allGood) StatusSuccess else StatusWarning), contentAlignment = Alignment.Center) {
                        Icon(if (allGood) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (allGood) "Tất cả đã hoàn tất ✅" else "Cần xử lý để bật VoLTE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (allGood) StatusSuccess else StatusWarning)
                        Text(uiState.verdict.overallSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                    }
                }
            }
        }

        // Main action button
        item {
            val allGood = uiState.simpleIsAllGood
            when (uiState.simpleFixStage) {
                SimpleFixStage.DONE_SUCCESS -> {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = StatusSuccess), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Hoàn tất — VoLTE đã sẵn sàng!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                    OutlinedButton(onClick = { viewModel.resetSimpleFix(); viewModel.loadInitialData() }, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Kiểm tra lại")
                    }
                }
                else -> {
                    if (allGood) {
                        Button(
                            onClick = { viewModel.resetSimpleFix() },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tất cả đã hoàn tất ✓", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Text("Không cần thao tác thêm. Nếu muốn thử lại luồng sửa, nhấn “Sửa ngay” bên dưới.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        OutlinedButton(onClick = { viewModel.onSimpleFixClicked(context) }, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("Thử luồng sửa (demo) →")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onSimpleFixClicked(context) },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.PhoneInTalk, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sửa ngay →", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        // Hint per chipset
                        val hint = when (uiState.chipsetInfo.type) {
                            ChipsetDetector.ChipsetType.MEDIATEK -> "Nhấn để tự động mở EngineerMode (MTK) và bật VoLTE trực tiếp."
                            ChipsetDetector.ChipsetType.QUALCOMM -> "Nhấn để hiện cảnh báo EFS → thử menu ẩn (*#800#, *#*#4636#*#*...) → nếu không được sẽ dùng Shizuku."
                            else -> "Nhấn để thử menu ẩn trước, nếu không được sẽ chuyển sang Shizuku + Pixel IMS."
                        }
                        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }

        // Stage: MTK trying
        if (uiState.simpleFixStage == SimpleFixStage.MTK_TRYING || (uiState.simpleFixStage == SimpleFixStage.DONE_SUCCESS && uiState.chipsetInfo.type == ChipsetDetector.ChipsetType.MEDIATEK && uiState.lastMtkLaunchMethod != null)) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.10f)), border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("MTK EngineerMode", fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                        Text(
                            if (uiState.simpleFixStage == SimpleFixStage.DONE_SUCCESS) "Đã gửi lệnh mở EngineerMode qua: ${uiState.lastMtkLaunchMethod}. Trong EngineerMode: vào Telephony → IMS → bật VoLTE / ViLTE / VoWiFi → Reboot."
                            else "Đang thử mở EngineerMode… Phương thức: ${uiState.lastMtkLaunchMethod ?: "đang chọn"}",
                            style = MaterialTheme.typography.bodySmall, lineHeight = 17.sp
                        )
                        if (uiState.simpleFixStage != SimpleFixStage.DONE_SUCCESS) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = StatusSuccess)
                                Spacer(Modifier.width(8.dp))
                                Text("Đang mở…", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Stage: QC trying menu
        if (uiState.simpleFixStage == SimpleFixStage.QC_TRYING_MENU) {
            item {
                val idx = uiState.hiddenMenuIndex.coerceIn(0, HiddenMenuLauncher.QUALCOMM_SEQUENCE.size - 1)
                val code = HiddenMenuLauncher.QUALCOMM_SEQUENCE.getOrNull(idx)
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = VoLtePrimary.copy(alpha = 0.08f)), border = BorderStroke(1.dp, VoLtePrimary.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VoLtePrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang thử menu ẩn ${idx + 1}/${HiddenMenuLauncher.QUALCOMM_SEQUENCE.size}", fontWeight = FontWeight.Bold, color = VoLtePrimary)
                        }
                        if (code != null) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(code.code, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = VoLtePrimary)
                                    Text(code.label + " • " + code.target, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(code.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                                }
                            }
                            Text("Đã mở bàn phím quay số với mã trên. Hãy kiểm tra xem có nút “VoLTE / Cuộc gọi 4G / Enhanced 4G LTE” hiện ra không.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { viewModel.onHiddenMenuSuccess(context) }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Có, đã thấy ✓", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                OutlinedButton(onClick = { viewModel.onHiddenMenuFailed(context) }, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Chưa thấy", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Stage: Need Shizuku
        if (uiState.simpleFixStage == SimpleFixStage.NEED_SHIZUKU) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, VoLtePrimary.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = VoLtePrimary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cần Shizuku + Pixel IMS", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = VoLtePrimary)
                        }
                        Text("Đã thử hết menu ẩn mà vẫn chưa hiện nút VoLTE. Bước tiếp theo sẽ dùng Shizuku để override CarrierConfig và bật IMS provisioning (persistent, không mất sau reboot).", style = MaterialTheme.typography.bodyMedium, lineHeight = 18.sp)
                        Surface(shape = RoundedCornerShape(12.dp), color = VoLtePrimary.copy(alpha = 0.08f)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Tự động theo mã máy:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("• Mã máy: ${uiState.deviceCode}", style = MaterialTheme.typography.bodySmall)
                                Text("• Chip: ${uiState.cpuLabel}", style = MaterialTheme.typography.bodySmall)
                                Text("• SIM: ${uiState.simSlots.firstOrNull()?.carrierName ?: "—"} • subId=${uiState.simSlots.firstOrNull()?.subscriptionId ?: 0}", style = MaterialTheme.typography.bodySmall)
                                Text("• Script: Pixel Full Enable (VoLTE+VoWiFi+VoNR+Vt) + IMS provisioning", style = MaterialTheme.typography.bodySmall, color = VoLtePrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        val shizukuReady = uiState.shizukuState is com.example.data.shizuku.ShizukuManager.ShizukuState.ReadyShell || uiState.shizukuState is com.example.data.shizuku.ShizukuManager.ShizukuState.ReadyRoot
                        if (!shizukuReady) {
                            Surface(shape = RoundedCornerShape(12.dp), color = StatusWarning.copy(alpha = 0.12f), border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f))) {
                                Text("Shizuku chưa sẵn sàng (${uiState.shizukuState}). Cần cài Shizuku và cấp quyền trước khi chạy.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { viewModel.requestShizukuPermission() }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary), modifier = Modifier.weight(1f)) {
                                    Text("Cấp quyền Shizuku", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                OutlinedButton(onClick = { viewModel.refreshShizukuState() }, shape = RoundedCornerShape(20.dp)) {
                                    Text("Làm mới", fontSize = 13.sp)
                                }
                            }
                            TextButton(onClick = { viewModel.openShizukuDownloadPage(context) }, modifier = Modifier.fillMaxWidth()) { Text("Tải Shizuku (shizuku.rikka.app)") }
                        } else {
                            Button(
                                onClick = { viewModel.runSimpleShizukuFix() },
                                enabled = !uiState.isRunningShizukuScript,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                if (uiState.isRunningShizukuScript) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Đang chạy…")
                                } else {
                                    Icon(Icons.Default.Security, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Chạy Shizuku Fix ngay", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (uiState.shizukuLogs.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0F1113), modifier = Modifier.fillMaxWidth()) {
                                    Text(uiState.shizukuLogs.take(2000), color = Color(0xFFB0D0FF), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(12.dp))
                                }
                            }
                            if (uiState.lastScriptResult != null) {
                                Text(uiState.lastScriptResult!!, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = StatusSuccess)
                            }
                        }
                    }
                }
            }
        }

        // Bottom: switch to classic + version
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bạn đang ở bản Lite — giao diện siêu đơn giản", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Muốn xem đầy đủ 7 tabs (Chẩn đoán, Band, Mã ẩn, Cẩm nang, Đăng ký, IMS, Shizuku) như bản gốc? ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onSwitchToClassic, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mở giao diện đầy đủ (Classic)")
                    }
                    Text("VoLTE Checker Lite v1.0.4 • fork riêng • không thay thế bản gốc • ${uiState.deviceCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SimpleRow(
    number: String,
    icon: ImageVector,
    color: Color,
    title: String,
    value: String,
    subtitle: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Text(number, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
