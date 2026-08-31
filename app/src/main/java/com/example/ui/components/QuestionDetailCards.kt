package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VolteVerdict
import com.example.ui.theme.VoLtePrimary
import com.example.ui.theme.VoLteSecondary
import com.example.ui.theme.VoLteTertiary

@Composable
fun QuestionDetailCards(
    verdict: VolteVerdict,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "CHI TIẾT 3 CÂU HỎI TRỌNG TÂM",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Card 1: Device Support
        AccordionDetailCard(
            title = "1. Máy có hỗ trợ VoLTE không?",
            icon = Icons.Default.PhoneAndroid,
            iconTint = VoLtePrimary,
            shortStatus = verdict.deviceSupported.name,
            reasonText = verdict.deviceSupportReason,
            extraTips = "VoLTE (Voice over LTE) yêu cầu modem 4G LTE/5G của chip xử lý (Snapdragon, MediaTek, Exynos, Tensor) và hệ điều hành Android 7.0 (Nougat) trở lên hỗ trợ chuẩn IMS stack.",
            initiallyExpanded = true
        )

        // Card 2: Is VoLTE Enabled
        AccordionDetailCard(
            title = "2. VoLTE đã được bật chưa?",
            icon = Icons.Default.PowerSettingsNew,
            iconTint = VoLteSecondary,
            shortStatus = verdict.isVolteEnabled.name,
            reasonText = verdict.enabledStatusReason,
            extraTips = "Để VoLTE hoạt động, cần 3 điều kiện đồng thời:\n• SIM đã kích hoạt VoLTE từ nhà mạng (qua tin nhắn hoặc tổng đài).\n• Điện thoại đã bật công tắc VoLTE trong Cài đặt SIM.\n• Đang ở khu vực có sóng 4G LTE ổn định.",
            initiallyExpanded = true
        )

        // Card 3: Hidden Option in Settings
        AccordionDetailCard(
            title = "3. Tùy chọn có bị ẩn khỏi Cài đặt không?",
            icon = Icons.Default.Visibility,
            iconTint = VoLteTertiary,
            shortStatus = verdict.settingsVisibility.name,
            reasonText = verdict.visibilityReason,
            extraTips = "Tại sao bị ẩn?\n• Xiaomi/Redmi/POCO: Mặc định bật Carrier Check. Chỉ cần gõ mã *#*#86583#*#* để bỏ khóa.\n• Google Pixel: Khóa theo vùng nạp sẵn. Có thể bật qua Shizuku + Pixel IMS.\n• Nhà mạng khóa: File cấu hình CarrierConfig đặt cờ ẩn nút gạt.",
            initiallyExpanded = true
        )
    }
}

@Composable
private fun AccordionDetailCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    shortStatus: String,
    reasonText: String,
    extraTips: String,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { expanded = !expanded }
            .testTag("detail_card_${title.take(15)}"),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = reasonText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = extraTips,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
