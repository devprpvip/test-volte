package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveStatus
import com.example.data.model.SupportStatus
import com.example.data.model.VisibilityStatus
import com.example.data.model.VolteVerdict
import com.example.ui.theme.LightOutline
import com.example.ui.theme.LightOutlineVariant
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusInfoText
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusSuccessText
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.theme.StatusWarningText
import com.example.ui.theme.VoLtePrimary
import com.example.ui.theme.VoLteSecondary

@Composable
fun VerdictHeroCard(
    verdict: VolteVerdict,
    onQuickFixClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllGood = verdict.deviceSupported == SupportStatus.SUPPORTED &&
            verdict.isVolteEnabled == ActiveStatus.ACTIVE_REGISTERED &&
            verdict.settingsVisibility == VisibilityStatus.VISIBLE

    val isHidden = verdict.settingsVisibility == VisibilityStatus.HIDDEN_BY_OEM ||
            verdict.settingsVisibility == VisibilityStatus.HIDDEN_BY_CARRIER ||
            verdict.settingsVisibility == VisibilityStatus.LOCKED_RESTRICTED

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Density Primary Hero Status Banner (#D3E4FF / rounded-[28px])
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verdict_hero_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAllGood) Color(0xFFD3E4FF) else if (isHidden) StatusWarningContainer else Color(0xFFE8DEF8)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isAllGood) LightOutlineVariant else if (isHidden) StatusWarning.copy(alpha = 0.4f) else Color(0xFFD0BCFF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TRẠNG THÁI VOLTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = if (isAllGood) Color(0xFF001D36) else if (isHidden) StatusWarningText else Color(0xFF21005D)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAllGood) "Đang hoạt động" else if (isHidden) "Cần mở khóa ẩn" else "Cần kích hoạt",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllGood) Color(0xFF001D36) else if (isHidden) StatusWarningText else Color(0xFF21005D)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = verdict.overallSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAllGood) Color(0xFF001D36).copy(alpha = 0.75f) else if (isHidden) StatusWarningText.copy(alpha = 0.8f) else Color(0xFF21005D).copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // High Density HD Emblem Icon
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "HD",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isAllGood) VoLtePrimary else if (isHidden) StatusWarning else VoLteSecondary
                        )
                    }
                }
            }
        }

        // High Density Diagnostics Card with System Settings Header & Telemetry Items
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8DEF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Chẩn đoán hệ thống 3 mục tiêu",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEADDFF)
                    ) {
                        Text(
                            text = if (isAllGood) "ĐẠT CHUẨN" else "CẦN XỬ LÝ",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF21005D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The 3 Direct Answers Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Hardware Support
                    AnswerItemRow(
                        question = "1. Hỗ trợ VoLTE phần cứng",
                        statusText = when (verdict.deviceSupported) {
                            SupportStatus.SUPPORTED -> "Có (Modem LTE & IMS)"
                            SupportStatus.PARTIALLY_SUPPORTED -> "Hỗ trợ một phần"
                            SupportStatus.NOT_SUPPORTED -> "Không hỗ trợ"
                            SupportStatus.UNKNOWN -> "Đang kiểm tra"
                        },
                        isPositive = verdict.deviceSupported == SupportStatus.SUPPORTED,
                        icon = when (verdict.deviceSupported) {
                            SupportStatus.SUPPORTED -> Icons.Default.CheckCircle
                            SupportStatus.NOT_SUPPORTED -> Icons.Default.Error
                            else -> Icons.Default.Warning
                        },
                        statusColor = when (verdict.deviceSupported) {
                            SupportStatus.SUPPORTED -> StatusSuccess
                            SupportStatus.NOT_SUPPORTED -> StatusError
                            else -> StatusWarning
                        }
                    )

                    // 2. Active Status
                    AnswerItemRow(
                        question = "2. VoLTE đã được bật chưa?",
                        statusText = when (verdict.isVolteEnabled) {
                            ActiveStatus.ACTIVE_REGISTERED -> "Đang hoạt động (IMS Registered)"
                            ActiveStatus.PROVISIONED_READY -> "Đã cấp phép / Sẵn sàng"
                            ActiveStatus.DISABLED -> "Chưa bật / Chưa đăng ký"
                            ActiveStatus.NOT_PROVISIONED -> "Chưa cấp phép dịch vụ"
                            ActiveStatus.UNKNOWN -> "Đang kiểm tra"
                        },
                        isPositive = verdict.isVolteEnabled == ActiveStatus.ACTIVE_REGISTERED,
                        icon = when (verdict.isVolteEnabled) {
                            ActiveStatus.ACTIVE_REGISTERED -> Icons.Default.PhoneInTalk
                            ActiveStatus.PROVISIONED_READY -> Icons.Default.Tune
                            else -> Icons.Default.Warning
                        },
                        statusColor = when (verdict.isVolteEnabled) {
                            ActiveStatus.ACTIVE_REGISTERED -> StatusSuccess
                            ActiveStatus.PROVISIONED_READY -> StatusInfo
                            else -> StatusWarning
                        }
                    )

                    // 3. Hidden Settings Visibility
                    AnswerItemRow(
                        question = "3. Tùy chọn trong Cài đặt",
                        statusText = when (verdict.settingsVisibility) {
                            VisibilityStatus.VISIBLE -> "Không bị ẩn (Công khai)"
                            VisibilityStatus.HIDDEN_BY_OEM -> "BỊ ẨN do khóa hãng (Xiaomi/Pixel)"
                            VisibilityStatus.HIDDEN_BY_CARRIER -> "BỊ ẨN bởi cấu hình nhà mạng"
                            VisibilityStatus.LOCKED_RESTRICTED -> "Bị giới hạn vùng / Cần mở khóa"
                            VisibilityStatus.UNKNOWN -> "Đang kiểm tra"
                        },
                        isPositive = verdict.settingsVisibility == VisibilityStatus.VISIBLE,
                        icon = when (verdict.settingsVisibility) {
                            VisibilityStatus.VISIBLE -> Icons.Default.Visibility
                            VisibilityStatus.HIDDEN_BY_OEM, VisibilityStatus.HIDDEN_BY_CARRIER -> Icons.Default.VisibilityOff
                            else -> Icons.Default.Lock
                        },
                        statusColor = when (verdict.settingsVisibility) {
                            VisibilityStatus.VISIBLE -> Color(0xFF006A6A)
                            VisibilityStatus.UNKNOWN -> StatusInfo
                            else -> StatusWarning
                        }
                    )
                }

                if (isHidden || verdict.isVolteEnabled != ActiveStatus.ACTIVE_REGISTERED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onQuickFixClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quick_fix_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoLtePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHidden) "Mở khóa làm hiện nút VoLTE ngay" else "Xem hướng dẫn kích hoạt VoLTE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerItemRow(
    question: String,
    statusText: String,
    isPositive: Boolean,
    icon: ImageVector,
    statusColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) statusColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
