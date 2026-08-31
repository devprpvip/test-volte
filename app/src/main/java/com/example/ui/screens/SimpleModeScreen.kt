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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
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
import androidx.compose.ui.text.style.TextOverflow
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

    if (uiState.showEfsWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEfsWarning() },
            icon = { Icon(Icons.Default.Warning, null, tint = StatusWarning, modifier = Modifier.size(28.dp)) },
            title = { Text("Cảnh báo phân vùng EFS — Qualcomm", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Thiết bị của bạn dùng chip Qualcomm Snapdragon. Phân vùng EFS chứa IMEI và cấu hình modem.",
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = true
                    )
                    Surface(shape = RoundedCornerShape(12.dp), color = StatusWarning.copy(alpha = 0.12f), border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f))) {
                        Text(
                            "⚠ Tuyệt đối KHÔNG can thiệp trực tiếp vào EFS qua setprop / QPST nếu không có backup. App này CHỈ thử mở menu ẩn hệ thống (*#800#, *#*#4636#*#*...) và sau đó dùng Shizuku + Pixel IMS (không chạm EFS thô).",
                            style = MaterialTheme.typography.bodySmall,
                            softWrap = true,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Text("Bạn có muốn tiếp tục thử mở menu ẩn không? Nếu menu ẩn không hiện nút VoLTE, app sẽ tự chuyển sang Shizuku (an toàn).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmEfsWarning(context) }, colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary)) {
                    Text("Đã hiểu, tiếp tục →", maxLines = 1)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEfsWarning() }) { Text("Hủy") }
            }
        )
    }

    // Responsive: dùng padding nhỏ hơn trên mobile hẹp, tránh tràn ngang
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: App title + Lite badge + switch - compact cho mobile
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VoLtePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("HD", fontWeight = FontWeight.ExtraBold, color = VoLtePrimary, fontSize = 15.sp, maxLines = 1)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = true)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "VoLTE Checker",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "LITE 1.0.4",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Bản siêu đơn giản • Tự động nhận diện chipset",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = true
                        )
                        Text(
                            "Fork riêng, không thay thế bản gốc 1.0.4",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onSwitchToClassic, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Auto chipset banner - stack vertical trên màn hẹp để không tràn
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = chipBg),
                border = BorderStroke(1.dp, chipColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Memory, null, tint = chipColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tự động nhận diện chipset",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = chipColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            when {
                                isMtk -> "MediaTek (MTK) — sẽ tự mở EngineerMode"
                                isQc -> "Qualcomm — cảnh báo EFS → thử menu ẩn"
                                else -> "${chip.label} — thử menu ẩn → Shizuku"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true,
                            lineHeight = 16.sp,
                            fontSize = 13.sp
                        )
                        Text(
                            "Phát hiện: ${uiState.cpuLabel} • ${chip.hardware.ifBlank { chip.board }} • ${chip.socModel.ifBlank { "—" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = chipColor)
                    } else {
                        Icon(
                            imageVector = if (isMtk || isQc) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = chipColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Permission banner - responsive, nút không tràn
        if (!uiState.hasPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Security, null, tint = StatusWarning, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Cấp quyền Điện thoại để đọc SIM & IMS chính xác",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    softWrap = true,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    "Không cấp quyền vẫn xem được, nhưng kết quả sẽ là giả định.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    softWrap = true,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cấp quyền", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 5-item status table
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhoneInTalk, null, tint = VoLtePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "BẢNG TRẠNG THÁI — 5 MỤC",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                            ActiveStatus.ACTIVE_REGISTERED -> "Đang hoạt động (IMS Registered)"
                            ActiveStatus.PROVISIONED_READY -> "Đã cấp phép / Sẵn sàng"
                            ActiveStatus.DISABLED -> "Chưa bật / Chưa đăng ký"
                            ActiveStatus.NOT_PROVISIONED -> "Chưa cấp phép"
                            ActiveStatus.UNKNOWN -> "Đang kiểm tra…"
                        },
                        subtitle = uiState.verdict.enabledStatusReason
                    )
                    SimpleRow(
                        number = "3",
                        icon = if (uiState.verdict.settingsVisibility == VisibilityStatus.VISIBLE) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        color = if (uiState.verdict.settingsVisibility == VisibilityStatus.VISIBLE) StatusSuccess else StatusWarning,
                        title = "Nút VoLTE trong Cài đặt",
                        value = when (uiState.verdict.settingsVisibility) {
                            VisibilityStatus.VISIBLE -> "Đang hiện (không ẩn)"
                            VisibilityStatus.HIDDEN_BY_OEM -> "BỊ ẨN do khóa hãng"
                            VisibilityStatus.HIDDEN_BY_CARRIER -> "BỊ ẨN bởi nhà mạng"
                            VisibilityStatus.LOCKED_RESTRICTED -> "Bị giới hạn vùng / cần mở khóa"
                            VisibilityStatus.UNKNOWN -> "Đang kiểm tra…"
                        },
                        subtitle = uiState.verdict.visibilityReason
                    )
                    SimpleRow(
                        number = "4",
                        icon = Icons.Default.Smartphone,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = "Mã máy",
                        value = "${uiState.deviceInfo.manufacturer} ${uiState.deviceInfo.model} • ${uiState.deviceInfo.brand}",
                        subtitle = "Board: ${uiState.deviceInfo.board} • HW: ${uiState.deviceInfo.hardware} • ${uiState.deviceInfo.androidVersion}"
                    )
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
                        subtitle = "SoC: ${uiState.chipsetInfo.socModel.ifBlank { uiState.chipsetInfo.hardware.ifBlank { "—" }}} • Platform: ${uiState.chipsetInfo.platform.ifBlank { "—" }}"
                    )
                }
            }
        }

        // Overall verdict banner - compact, wrap
        item {
            val allGood = uiState.simpleIsAllGood
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (allGood) StatusSuccess.copy(alpha = 0.12f) else StatusWarning.copy(alpha = 0.14f)),
                border = BorderStroke(1.dp, if (allGood) StatusSuccess.copy(alpha = 0.35f) else StatusWarning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (allGood) StatusSuccess else StatusWarning),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (allGood) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (allGood) "Tất cả đã hoàn tất" else "Cần xử lý để bật VoLTE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (allGood) StatusSuccess else StatusWarning,
                            softWrap = true,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            uiState.verdict.overallSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp,
                            softWrap = true,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Main action button - responsive
        item {
            val allGood = uiState.simpleIsAllGood
            when (uiState.simpleFixStage) {
                SimpleFixStage.DONE_SUCCESS -> {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = StatusSuccess), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Hoàn tất — VoLTE đã sẵn sàng!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetSimpleFix(); viewModel.loadInitialData() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text("Kiểm tra lại", maxLines = 1)
                    }
                }
                else -> {
                    if (allGood) {
                        Button(
                            onClick = { viewModel.resetSimpleFix() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tất cả đã hoàn tất ✓", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            "Không cần thao tác thêm. Nếu muốn thử lại luồng sửa, nhấn “Sửa ngay” bên dưới.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        OutlinedButton(
                            onClick = { viewModel.onSimpleFixClicked(context) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Thử luồng sửa (demo) →", maxLines = 1)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.onSimpleFixClicked(context) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.PhoneInTalk, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sửa ngay →", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1)
                        }
                        val hint = when (uiState.chipsetInfo.type) {
                            ChipsetDetector.ChipsetType.MEDIATEK -> "Nhấn để tự động mở EngineerMode (MTK) và bật VoLTE trực tiếp."
                            ChipsetDetector.ChipsetType.QUALCOMM -> "Nhấn để hiện cảnh báo EFS → thử menu ẩn (*#800#, *#*#4636#*#*...) → nếu không được sẽ dùng Shizuku."
                            else -> "Nhấn để thử menu ẩn trước, nếu không được sẽ chuyển sang Shizuku + Pixel IMS."
                        }
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        // Stage: MTK trying
        if (uiState.simpleFixStage == SimpleFixStage.MTK_TRYING || (uiState.simpleFixStage == SimpleFixStage.DONE_SUCCESS && uiState.chipsetInfo.type == ChipsetDetector.ChipsetType.MEDIATEK && uiState.lastMtkLaunchMethod != null)) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.10f)),
                    border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Memory, null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("MTK EngineerMode", fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Text(
                            if (uiState.simpleFixStage == SimpleFixStage.DONE_SUCCESS) "Đã gửi lệnh mở EngineerMode qua: ${uiState.lastMtkLaunchMethod}. Trong EngineerMode: vào Telephony → IMS → bật VoLTE / ViLTE / VoWiFi → Reboot."
                            else "Đang thử mở EngineerMode… Phương thức: ${uiState.lastMtkLaunchMethod ?: "đang chọn"}",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            softWrap = true,
                            fontSize = 11.sp
                        )
                        if (uiState.simpleFixStage != SimpleFixStage.DONE_SUCCESS) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = StatusSuccess)
                                Spacer(Modifier.width(8.dp))
                                Text("Đang mở…", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VoLtePrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, VoLtePrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VoLtePrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Đang thử menu ẩn ${idx + 1}/${HiddenMenuLauncher.QUALCOMM_SEQUENCE.size}",
                                fontWeight = FontWeight.Bold,
                                color = VoLtePrimary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (code != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(code.code, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = VoLtePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(code.label + " • " + code.target, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, softWrap = true, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(code.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp, softWrap = true, fontSize = 11.sp)
                                }
                            }
                            Text(
                                "Đã mở bàn phím quay số với mã trên. Hãy kiểm tra xem có nút “VoLTE / Cuộc gọi 4G / Enhanced 4G LTE” hiện ra không.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true,
                                fontSize = 11.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.onHiddenMenuSuccess(context) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Có, đã thấy", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.onHiddenMenuFailed(context) },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Chưa thấy", fontSize = 12.sp, maxLines = 1)
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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, VoLtePrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Security, null, tint = VoLtePrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cần Shizuku + Pixel IMS", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = VoLtePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Text(
                            "Đã thử hết menu ẩn mà vẫn chưa hiện nút VoLTE. Bước tiếp theo sẽ dùng Shizuku để override CarrierConfig và bật IMS provisioning (persistent, không mất sau reboot).",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            softWrap = true,
                            fontSize = 11.sp
                        )
                        Surface(shape = RoundedCornerShape(12.dp), color = VoLtePrimary.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Tự động theo mã máy:", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                Text("• Mã máy: ${uiState.deviceCode}", style = MaterialTheme.typography.bodySmall, softWrap = true, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("• Chip: ${uiState.cpuLabel}", style = MaterialTheme.typography.bodySmall, softWrap = true, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("• SIM: ${uiState.simSlots.firstOrNull()?.carrierName ?: "—"} • subId=${uiState.simSlots.firstOrNull()?.subscriptionId ?: 0}", style = MaterialTheme.typography.bodySmall, softWrap = true, fontSize = 11.sp)
                                Text("• Script: Pixel Full Enable (VoLTE+VoWiFi+VoNR+Vt) + IMS provisioning", style = MaterialTheme.typography.bodySmall, color = VoLtePrimary, fontWeight = FontWeight.SemiBold, softWrap = true, fontSize = 11.sp)
                            }
                        }
                        val shizukuReady = uiState.shizukuState is com.example.data.shizuku.ShizukuManager.ShizukuState.ReadyShell || uiState.shizukuState is com.example.data.shizuku.ShizukuManager.ShizukuState.ReadyRoot
                        if (!shizukuReady) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusWarning.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Shizuku chưa sẵn sàng (${uiState.shizukuState}). Cần cài Shizuku và cấp quyền trước khi chạy.", style = MaterialTheme.typography.bodySmall, softWrap = true, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.requestShizukuPermission() },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cấp quyền Shizuku", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                                OutlinedButton(onClick = { viewModel.refreshShizukuState() }, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("Làm mới", fontSize = 12.sp)
                                }
                            }
                            TextButton(onClick = { viewModel.openShizukuDownloadPage(context) }, modifier = Modifier.fillMaxWidth()) { Text("Tải Shizuku (shizuku.rikka.app)", fontSize = 12.sp, maxLines = 1) }
                        } else {
                            Button(
                                onClick = { viewModel.runSimpleShizukuFix() },
                                enabled = !uiState.isRunningShizukuScript,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (uiState.isRunningShizukuScript) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Đang chạy…", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Security, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Chạy Shizuku Fix ngay", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (uiState.shizukuLogs.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0F1113), modifier = Modifier.fillMaxWidth()) {
                                    Text(uiState.shizukuLogs.take(2000), color = Color(0xFFB0D0FF), style = MaterialTheme.typography.labelSmall, softWrap = true, fontSize = 10.sp, modifier = Modifier.padding(10.dp))
                                }
                            }
                            if (uiState.lastScriptResult != null) {
                                Text(uiState.lastScriptResult!!, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = StatusSuccess, softWrap = true, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom: switch to classic + version
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Bạn đang ở bản Lite — giao diện siêu đơn giản",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Muốn xem đầy đủ 7 tabs (Chẩn đoán, Band, Mã ẩn, Cẩm nang, Đăng ký, IMS, Shizuku) như bản gốc? ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = onSwitchToClassic,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mở giao diện đầy đủ (Classic)", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        "VoLTE Checker Lite v1.0.4 • fork riêng • branch lite-1.0.4 • ${uiState.deviceCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
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
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 11.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 15.sp,
                softWrap = true,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    softWrap = true,
                    fontSize = 11.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
            }
        }
    }
}
