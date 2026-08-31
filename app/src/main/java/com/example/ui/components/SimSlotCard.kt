package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SimSlotInfo
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VoLtePrimary
import com.example.ui.theme.VoLteSecondary

@Composable
fun SimSlotCard(
    simInfo: SimSlotInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sim_slot_card_${simInfo.slotIndex}"),
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
            // SIM Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEADDFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = null,
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "KHE SIM ${simInfo.slotIndex + 1}: ${simInfo.carrierName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (simInfo.mccMnc.isNotBlank()) {
                            Text(
                                text = "Mã mạng: ${simInfo.mccMnc} | ${simInfo.countryIso.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD3E4FF)
                ) {
                    Text(
                        text = simInfo.networkType,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF001D36)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // SIM Metrics Grid in rounded High Density surface container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SimMetricRow(
                        label = "Đăng ký phiên IMS (IMS Registered)",
                        value = when (simInfo.isImsRegistered) {
                            true -> "ĐÃ ĐĂNG KÝ (VoLTE Active)"
                            false -> "Chưa đăng ký"
                            null -> "Đã cấp phép / Sẵn sàng"
                        },
                        statusColor = when (simInfo.isImsRegistered) {
                            true -> StatusSuccess
                            false -> StatusWarning
                            null -> StatusInfo
                        }
                    )

                    SimMetricRow(
                        label = "Tính năng VoLTE khả dụng",
                        value = when (simInfo.isVoiceOverLteAvailable) {
                            true -> "Khả dụng trên thiết bị"
                            false -> "Chưa khả dụng"
                            null -> "Tương thích mạng 4G"
                        },
                        statusColor = when (simInfo.isVoiceOverLteAvailable) {
                            true -> StatusSuccess
                            false -> StatusWarning
                            null -> StatusInfo
                        }
                    )

                    SimMetricRow(
                        label = "Quyền chỉnh sửa nút VoLTE",
                        value = when (simInfo.isEnhanced4gLteEditable) {
                            true -> "Cho phép bật/tắt (Công khai)"
                            false -> "BỊ KHÓA / ẨN bởi cấu hình"
                            null -> "Mặc định hệ điều hành"
                        },
                        statusColor = when (simInfo.isEnhanced4gLteEditable) {
                            true -> StatusSuccess
                            false -> StatusWarning
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    SimMetricRow(
                        label = "Chế độ Chuyển vùng (Roaming)",
                        value = if (simInfo.isRoaming) "Đang bật chuyển vùng" else "Tắt (Mạng nội địa)",
                        statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SimMetricRow(
    label: String,
    value: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

