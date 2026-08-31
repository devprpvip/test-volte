package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.shizuku.ShizukuManager
import com.example.data.shizuku.VolteActivationScripts
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VoLtePrimary
import com.example.ui.theme.VoLteSecondary

@Composable
fun ShizukuStatusCard(
    state: ShizukuManager.ShizukuState,
    isInstalled: Boolean,
    isRunningScript: Boolean,
    lastResult: String?,
    logs: String,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onDownloadShizuku: () -> Unit,
    onOpenWirelessDebugging: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().testTag("shizuku_status_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(VoLtePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = VoLtePrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("KÍCH HOẠT NÂNG CAO (SHIZUKU)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Chạy script đặc quyền khi tùy chọn bị khóa sâu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = VoLtePrimary)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // State indicator
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when (state) {
                    is ShizukuManager.ShizukuState.ReadyRoot, is ShizukuManager.ShizukuState.ReadyShell -> StatusSuccess.copy(alpha = 0.12f)
                    is ShizukuManager.ShizukuState.PermissionDenied -> StatusWarning.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = BorderStroke(1.dp, when (state) {
                    is ShizukuManager.ShizukuState.ReadyRoot, is ShizukuManager.ShizukuState.ReadyShell -> StatusSuccess.copy(alpha = 0.4f)
                    is ShizukuManager.ShizukuState.PermissionDenied -> StatusWarning.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                }),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (state) {
                            is ShizukuManager.ShizukuState.ReadyRoot, is ShizukuManager.ShizukuState.ReadyShell -> Icons.Default.CheckCircle
                            is ShizukuManager.ShizukuState.PermissionDenied -> Icons.Default.Warning
                            is ShizukuManager.ShizukuState.NotRunning -> Icons.Default.Error
                            is ShizukuManager.ShizukuState.NotInstalled -> Icons.Default.Error
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (state) {
                            is ShizukuManager.ShizukuState.ReadyRoot, is ShizukuManager.ShizukuState.ReadyShell -> StatusSuccess
                            is ShizukuManager.ShizukuState.PermissionDenied -> StatusWarning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (state) {
                                is ShizukuManager.ShizukuState.ReadyRoot -> "✅ Shizuku sẵn sàng (ROOT - uid 0)"
                                is ShizukuManager.ShizukuState.ReadyShell -> "✅ Shizuku sẵn sàng (ADB - uid shell 2000)"
                                is ShizukuManager.ShizukuState.PermissionDenied -> "⚠ Shizuku đang chạy nhưng CHƯA cấp quyền"
                                is ShizukuManager.ShizukuState.NotRunning -> if (!isInstalled) "❌ Chưa cài Shizuku" else "⏸ Shizuku chưa chạy"
                                is ShizukuManager.ShizukuState.NotInstalled -> "❌ Chưa cài Shizuku"
                                else -> "Đang kiểm tra Shizuku..."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (state) {
                                is ShizukuManager.ShizukuState.ReadyRoot -> "Có thể chạy mọi script đặc quyền, kể cả setprop persist.* và override carrier config."
                                is ShizukuManager.ShizukuState.ReadyShell -> "Có thể chạy hầu hết script (settings, cmd phone). Một số lệnh setprop cần ROOT có thể báo denied."
                                is ShizukuManager.ShizukuState.PermissionDenied -> "Nhấn 'Cấp quyền Shizuku' và chọn 'Allow all the time'."
                                is ShizukuManager.ShizukuState.NotRunning -> if (!isInstalled) "Cài Shizuku từ Play Store hoặc shizuku.rikka.app" else "Mở Shizuku → Ghép nối (Wireless debugging) → Start."
                                else -> "Shizuku cho phép app gọi API hệ thống với quyền ADB mà không cần root."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Action buttons row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                when (state) {
                    is ShizukuManager.ShizukuState.PermissionDenied -> {
                        Button(onClick = onRequestPermission, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary)) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cấp quyền Shizuku", fontWeight = FontWeight.Bold)
                        }
                    }
                    is ShizukuManager.ShizukuState.NotRunning, is ShizukuManager.ShizukuState.NotInstalled -> {
                        if (!isInstalled) {
                            Button(onClick = onDownloadShizuku, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary)) {
                                Text("Tải Shizuku", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(onClick = onOpenWirelessDebugging, shape = RoundedCornerShape(20.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mở Wireless Debugging")
                            }
                            Button(onClick = onRefresh, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = VoLteSecondary)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kiểm tra lại")
                            }
                        }
                    }
                    else -> {}
                }
            }

            // How-to expand
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📖 Cách kích hoạt Shizuku (không cần PC, Android 11+):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("1. Cài Shizuku từ Play Store (moe.shizuku.privileged.api)\n2. Mở Shizuku → 'Pairing' → 'Developer options' → Bật 'Wireless debugging'\n3. Trong Wireless debugging → 'Pair device with pairing code' → nhập mã vào Shizuku\n4. Quay lại Shizuku → 'Start' → Đợi hiện 'Shizuku is running'\n5. Quay lại app này → 'Cấp quyền Shizuku' → Allow all the time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                    Text("PC (ADB): adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = VoLtePrimary, modifier = Modifier.padding(top = 4.dp))
                    Text("Lưu ý Oct 2025: Android 16 QPR2+ chặn overrideConfig persistent. App sẽ tự fallback sang IMS provisioning (persistent).", style = MaterialTheme.typography.labelSmall, color = StatusWarning, fontWeight = FontWeight.SemiBold)
                }
            }

            if (isRunningScript) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(StatusWarning.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VoLtePrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Đang chạy script đặc quyền...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            lastResult?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = StatusSuccess.copy(alpha = 0.10f), border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (logs.isNotBlank()) {
                Text("Nhật ký thực thi:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1A1C1E), border = BorderStroke(1.dp, Color(0xFF2A2E31)), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = logs,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFB0D0FF),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(14.dp).verticalScroll(rememberScrollState()).height(180.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShizukuScriptCard(
    script: VolteActivationScripts.ScriptItem,
    isReady: Boolean,
    isRunning: Boolean,
    onRun: (VolteActivationScripts.ScriptItem) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().testTag("shizuku_script_${script.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(when (script.type) {
                    VolteActivationScripts.ScriptType.CARRIER_CONFIG_OVERRIDE -> VoLtePrimary.copy(alpha = 0.12f)
                    VolteActivationScripts.ScriptType.TELEPHONY_PROVISIONING -> StatusSuccess.copy(alpha = 0.12f)
                    VolteActivationScripts.ScriptType.SHELL_PROP -> StatusWarning.copy(alpha = 0.12f)
                    else -> VoLteSecondary.copy(alpha = 0.12f)
                }), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (script.type) {
                            VolteActivationScripts.ScriptType.CARRIER_CONFIG_OVERRIDE -> Icons.Default.Security
                            VolteActivationScripts.ScriptType.TELEPHONY_PROVISIONING -> Icons.Default.CheckCircle
                            VolteActivationScripts.ScriptType.SHELL_PROP -> Icons.Default.Terminal
                            else -> Icons.Default.Settings
                        },
                        contentDescription = null,
                        tint = when (script.type) {
                            VolteActivationScripts.ScriptType.CARRIER_CONFIG_OVERRIDE -> VoLtePrimary
                            VolteActivationScripts.ScriptType.TELEPHONY_PROVISIONING -> StatusSuccess
                            VolteActivationScripts.ScriptType.SHELL_PROP -> StatusWarning
                            else -> VoLteSecondary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(script.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(script.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                }
            }

            // Commands preview
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0F1113), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    script.commands.take(4).forEach { cmd ->
                        Row {
                            Text("$ ", color = StatusSuccess, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                            Text(cmd, color = Color(0xFFD0D4DA), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, lineHeight = 14.sp)
                        }
                    }
                    if (script.commands.size > 4) {
                        Text("+ ${script.commands.size - 4} lệnh nữa...", color = Color(0xFF8A9199), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            script.warning?.let { w ->
                Surface(shape = RoundedCornerShape(12.dp), color = StatusWarning.copy(alpha = 0.12f), border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(w, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, lineHeight = 14.sp)
                    }
                }
            }

            requiresRootBadge(script.requiresRoot)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onRun(script) },
                    enabled = isReady && !isRunning,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Đang chạy...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chạy qua Shizuku", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                OutlinedButton(
                    onClick = { onCopy(script.commands.joinToString("\n")) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.width(110.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sao chép", fontSize = 12.sp)
                }
            }
            if (!isReady) {
                Text("Cần Shizuku Ready mới chạy được.", style = MaterialTheme.typography.labelSmall, color = StatusError)
            }
        }
    }
}

@Composable
private fun requiresRootBadge(requiresRoot: Boolean) {
    if (!requiresRoot) return
    Surface(shape = RoundedCornerShape(20.dp), color = StatusError.copy(alpha = 0.12f), border = BorderStroke(1.dp, StatusError.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Yêu cầu ROOT (uid 0)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = StatusError)
        }
    }
}
