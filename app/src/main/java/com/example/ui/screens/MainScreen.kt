package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.band.BandCheckManager
import com.example.data.shizuku.ShizukuManager
import com.example.data.shizuku.VolteActivationScripts
import com.example.ui.components.BrandGuideCard
import com.example.ui.components.CarrierSmsCard
import com.example.ui.components.QuestionDetailCards
import com.example.ui.components.SecretCodeCard
import com.example.ui.components.ShizukuScriptCard
import com.example.ui.components.ShizukuStatusCard
import com.example.ui.components.SimSlotCard
import com.example.ui.components.TechnicalDetailsView
import com.example.ui.theme.Accent
import com.example.ui.theme.Background
import com.example.ui.theme.Border
import com.example.ui.theme.Danger
import com.example.ui.theme.Success
import com.example.ui.theme.Surface
import com.example.ui.theme.Text
import com.example.ui.theme.TextMuted
import com.example.ui.theme.Warning
import com.example.ui.viewmodel.VolteCheckerViewModel

// YAML minimal design tokens
private val RadiusLg = 12.dp
private val RadiusMd = 8.dp
private val RadiusPill = 9999.dp
private val CardShadow = 1.dp
private val SectionGap = 64.dp
private val CardGap = 16.dp
private val InnerPadding = 24.dp

@Composable
private fun MinimalCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(RadiusLg),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        shadowElevation = CardShadow,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(InnerPadding)) { content() }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(RadiusMd),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White, disabledContainerColor = TextMuted.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier.height(44.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(RadiusMd),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier.height(44.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VolteCheckerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> viewModel.onPermissionResult(isGranted) }

    // Minimal tabs: Tổng quan | Band | Sửa lỗi | Shizuku | Khác
    val tabTitles = listOf(
        "Tổng quan" to Icons.Default.Phone,
        "Band & Quốc gia" to Icons.Default.SignalCellularAlt,
        "Sửa lỗi" to Icons.Default.Smartphone,
        "Shizuku" to Icons.Default.Security,
        "Khác" to Icons.Default.Tune
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Navigation Header 56dp, border bottom, surface
        Surface(
            color = Surface,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 1.dp, color = Border)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "check volte",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Text,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "alpha",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.background(Border, RoundedCornerShape(RadiusPill)).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { viewModel.loadInitialData() }, modifier = Modifier.testTag("refresh_button").size(44.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.openMobileNetworkSettings(context) }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = "Cài đặt", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Tabs - minimal pill style
        ScrollableTabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = Background,
            contentColor = Accent,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (uiState.selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = Accent,
                        height = 2.dp
                    )
                }
            },
            divider = { HorizontalDivider(color = Border, thickness = 1.dp) }
        ) {
            tabTitles.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { viewModel.setTab(index) },
                    modifier = Modifier.testTag("tab_$index"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (uiState.selectedTab == index) Accent else TextMuted)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = title,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (uiState.selectedTab == index) Accent else TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                )
            }
        }

        // Permission banner minimal
        if (!uiState.hasPermission) {
            Surface(
                shape = RoundedCornerShape(RadiusMd),
                color = Warning.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Warning.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Warning, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cần quyền Điện thoại để đọc SIM", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Text)
                        Text("Giúp kiểm tra band & VoLTE chính xác.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    PrimaryButton(text = "Cấp quyền", onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) })
                }
            }
        }

        // Content - centered max-width 960
        Box(
            modifier = Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 960.dp).fillMaxSize()) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text("Đang kiểm tra VoLTE & band…", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                } else {
                    when (uiState.selectedTab) {
                        0 -> OverviewTab(uiState, viewModel, onGoBand = { viewModel.setTab(1) }, onGoFix = { viewModel.setTab(2) })
                        1 -> BandTab(uiState, viewModel)
                        2 -> FixTab(uiState, viewModel)
                        3 -> ShizukuMinimalTab(uiState, viewModel)
                        4 -> OtherTab(uiState, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    viewModel: VolteCheckerViewModel,
    onGoBand: () -> Unit,
    onGoFix: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("diagnostics_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(CardGap)
    ) {
        // Hero Status Badge - 60-80% width, centered
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MinimalCard(modifier = Modifier.widthIn(max = 560.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        // Icon status
                        val isActive = uiState.verdict.isVolteEnabled.name == "ACTIVE_REGISTERED"
                        val isSupported = uiState.verdict.deviceSupported.name == "SUPPORTED"
                        Box(
                            modifier = Modifier.size(48.dp).background(
                                when {
                                    isActive -> Success.copy(alpha = 0.12f)
                                    !isSupported -> Danger.copy(alpha = 0.12f)
                                    else -> Warning.copy(alpha = 0.12f)
                                }, RoundedCornerShape(999.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isActive -> Icons.Default.CheckCircle
                                    !isSupported -> Icons.Default.Close
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when {
                                    isActive -> Success
                                    !isSupported -> Danger
                                    else -> Warning
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = when {
                                isActive -> "VoLTE Active"
                                isSupported -> "Sẵn sàng"
                                else -> "Không hỗ trợ"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Text
                        )
                        Text(
                            text = uiState.verdict.overallSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            PrimaryButton(text = "Kiểm tra Band", onClick = onGoBand)
                            SecondaryButton(text = "Sửa lỗi", onClick = onGoFix)
                        }
                    }
                }
            }
        }

        // SIM cards minimal
        if (uiState.simSlots.isNotEmpty()) {
            item {
                Text("SIM & Mạng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text, modifier = Modifier.padding(horizontal = 4.dp))
            }
            items(uiState.simSlots) { sim ->
                SimSlotCard(simInfo = sim)
            }
        }

        // Quick info + band preview
        item {
            uiState.bandResult?.let { bandRes ->
                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Band & Quốc gia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                        HorizontalDivider(color = Border)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Thiết bị", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text("${bandRes.device.manufacturer} ${bandRes.device.model}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Text)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Quốc gia", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text("${bandRes.country.countryName} (${bandRes.country.countryIso.uppercase()})", style = MaterialTheme.typography.bodyMedium, color = Text)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Band hiện tại", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(bandRes.band.bandLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = if (bandRes.compatible) Success else Warning)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Nhà mạng", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(bandRes.carrierMatch?.carrierName ?: bandRes.country.operatorName ?: "—", style = MaterialTheme.typography.bodyMedium, color = Text)
                            }
                        }
                        if (!bandRes.compatible && bandRes.band.isDetected) {
                            Surface(shape = RoundedCornerShape(RadiusMd), color = Warning.copy(alpha = 0.08f), border = androidx.compose.foundation.BorderStroke(1.dp, Warning.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                                Text("Band hiện tại có thể không tối ưu cho VoLTE. Xem tab Sửa lỗi để chọn band B3/B1/B8.", style = MaterialTheme.typography.bodySmall, color = Text, modifier = Modifier.padding(12.dp))
                            }
                        }
                        TextButton(onClick = onGoBand) { Text("Xem chi tiết Band →", color = Accent, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }

        // 3 questions
        item { QuestionDetailCards(verdict = uiState.verdict) }

        // Testing guide minimal
        item {
            MinimalCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cách nhận biết VoLTE đang hoạt động", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                    Text("• Biểu tượng VoLTE/HD cạnh cột sóng 4G\n• Gọi điện vẫn giữ 4G (không tụt 3G)\n• Vừa gọi vừa lướt web được\n• Kết nối 1–2s, âm thanh HD", style = MaterialTheme.typography.bodyMedium, color = TextMuted, lineHeight = 22.sp)
                }
            }
        }

        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
private fun BandTab(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    viewModel: VolteCheckerViewModel
) {
    val result = uiState.bandResult
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("band_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(CardGap)
    ) {
        item {
            MinimalCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kiểm tra Band & Quốc gia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                    }
                    Text("Xác định băng tần LTE/5G đang dùng và so khớp với nhà mạng tại quốc gia của bạn. Cần quyền Vị trí + Điện thoại để đọc band thực (nếu thiếu, hiển thị giả định).", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    if (result == null) {
                        Text("Đang tải dữ liệu band…", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }

        result?.let { r ->
            item {
                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Thiết bị & Hệ điều hành", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                        HorizontalDivider(color = Border)
                        BandRow(label = "Nhà sản xuất", value = r.device.manufacturer)
                        BandRow(label = "Model", value = r.device.model)
                        BandRow(label = "Android", value = r.device.androidVersion)
                        BandRow(label = "Thương hiệu", value = r.device.brand)
                    }
                }
            }
            item {
                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Quốc gia & Nhà mạng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                        HorizontalDivider(color = Border)
                        BandRow(label = "Quốc gia", value = "${r.country.countryName} (${r.country.countryIso.uppercase()})")
                        BandRow(label = "MCC", value = r.country.mcc ?: "—")
                        BandRow(label = "Nhà mạng", value = r.country.operatorName ?: r.carrierMatch?.carrierName ?: "Chưa xác định")
                        BandRow(label = "Mạng hỗ trợ", value = r.carrierMatch?.let { it.bands.joinToString { b -> "B${b.band} (${b.frequency})" } } ?: "—")
                        BandRow(label = "VoLTE bands", value = r.carrierMatch?.volteBands?.joinToString { "B$it" } ?: "B3, B1, B8 (mặc định VN)")
                    }
                }
            }
            item {
                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Băng tần hiện tại", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                            Surface(
                                shape = RoundedCornerShape(RadiusPill),
                                color = when (r.supportLevel) {
                                    BandCheckManager.SupportLevel.FULL -> Success.copy(alpha = 0.12f)
                                    BandCheckManager.SupportLevel.PARTIAL -> Warning.copy(alpha = 0.12f)
                                    else -> Border
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, when (r.supportLevel) {
                                    BandCheckManager.SupportLevel.FULL -> Success.copy(alpha = 0.3f)
                                    BandCheckManager.SupportLevel.PARTIAL -> Warning.copy(alpha = 0.3f)
                                    else -> Border
                                })
                            ) {
                                Text(
                                    text = when (r.supportLevel) {
                                        BandCheckManager.SupportLevel.FULL -> "Tương thích"
                                        BandCheckManager.SupportLevel.PARTIAL -> "Hạn chế"
                                        BandCheckManager.SupportLevel.UNKNOWN -> "Chưa rõ"
                                        BandCheckManager.SupportLevel.NO_BAND -> "Không xác định"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (r.supportLevel) {
                                        BandCheckManager.SupportLevel.FULL -> Success
                                        BandCheckManager.SupportLevel.PARTIAL -> Warning
                                        else -> TextMuted
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Border)
                        BandRow(label = "Band", value = r.band.bandLabel)
                        BandRow(label = "EARFCN/NRARFCN", value = r.band.earfcn?.toString() ?: r.band.nrarfcn?.toString() ?: r.band.channelNumber?.toString() ?: "—")
                        BandRow(label = "RAT", value = r.band.ratType)
                        BandRow(label = "Đọc thực?", value = if (r.band.isDetected) "Có (từ modem)" else "Giả định (cần quyền Vị trí)")
                        if (!r.band.isDetected) {
                            Surface(shape = RoundedCornerShape(RadiusMd), color = Warning.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                                Text("Cấp quyền Vị trí chính xác + Điện thoại để đọc band thực qua CellInfo. Hiện tại hiển thị B3 giả định cho VN.", style = MaterialTheme.typography.bodySmall, color = Text, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
            item {
                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Hướng fix cho ${r.device.manufacturer} ${r.device.model}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                        Text(r.fixGuide?.summary ?: "Áp dụng hướng chung.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        HorizontalDivider(color = Border)
                        r.fixGuide?.steps?.forEachIndexed { idx, step ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${idx + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Accent)
                                Text(step, style = MaterialTheme.typography.bodyMedium, color = Text, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            r.fixGuide?.secretCode?.let { code ->
                                SecondaryButton(text = "Gọi $code", onClick = { viewModel.dialCode(context, code) })
                            }
                            r.fixGuide?.shizukuScriptId?.let {
                                PrimaryButton(text = "Chạy Shizuku", onClick = { viewModel.setTab(3) })
                            }
                        }
                        r.fixGuide?.warning?.let { w ->
                            Text("⚠ $w", style = MaterialTheme.typography.bodySmall, color = Warning, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
private fun BandRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Text, modifier = Modifier.weight(1.2f))
    }
}

@Composable
private fun FixTab(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    viewModel: VolteCheckerViewModel
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("fix_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(CardGap)
    ) {
        item {
            MinimalCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sửa lỗi cho mọi dòng máy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                    Text("Mỗi hãng có cách khóa VoLTE khác nhau. Chọn hãng bên dưới hoặc xem hướng cho máy bạn (đã tự nhận diện).", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }

        // Current device fix (if bandResult available)
        uiState.bandResult?.fixGuide?.let { fix ->
            item {
                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Đề xuất cho máy bạn: ${uiState.bandResult.device.manufacturer} ${uiState.bandResult.device.model}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Accent)
                        Text(fix.summary, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        HorizontalDivider(color = Border)
                        fix.steps.forEachIndexed { idx, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(24.dp).background(Accent.copy(alpha = 0.1f), RoundedCornerShape(999.dp)), contentAlignment = Alignment.Center) {
                                    Text("${idx + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Accent)
                                }
                                Text(step, style = MaterialTheme.typography.bodyMedium, color = Text, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fix.secretCode?.let { SecondaryButton(text = it, onClick = { viewModel.dialCode(context, it) }) }
                        }
                    }
                }
            }
        }

        // All brands
        items(uiState.brandGuides) { guide ->
            BrandGuideCard(guide = guide, onDialSecretCode = { viewModel.dialCode(context, it) })
        }

        // Secret codes filter
        item {
            MinimalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Mã bí mật liên quan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tất cả", "Xiaomi", "Samsung", "Oppo", "Pixel", "Universal").forEach { brand ->
                            FilterChip(
                                selected = uiState.activeBrandFilter == brand,
                                onClick = { viewModel.setBrandFilter(brand) },
                                label = { Text(brand, fontSize = 13.sp) },
                                shape = RoundedCornerShape(RadiusPill),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = Color.White),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = uiState.activeBrandFilter == brand, borderColor = Border)
                            )
                        }
                    }
                    val filtered = uiState.secretCodes.filter { if (uiState.activeBrandFilter == "Tất cả") true else it.targetBrand.contains(uiState.activeBrandFilter, ignoreCase = true) }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtered.forEach { item ->
                            SecretCodeCard(item = item, onDialClick = { viewModel.dialCode(context, it) }, onCopyClick = { viewModel.copyToClipboard(context, it) })
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
private fun ShizukuMinimalTab(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    viewModel: VolteCheckerViewModel
) {
    val context = LocalContext.current
    val isReady = uiState.shizukuState is ShizukuManager.ShizukuState.ReadyShell || uiState.shizukuState is ShizukuManager.ShizukuState.ReadyRoot
    val subIds = uiState.simSlots.mapNotNull { if (it.subscriptionId != -1) it.subscriptionId else null }.ifEmpty { listOf(0) }
    val scripts = androidx.compose.runtime.remember(subIds) { VolteActivationScripts.allScripts(subIds) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("shizuku_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(CardGap)
    ) {
        item {
            ShizukuStatusCard(
                state = uiState.shizukuState,
                isInstalled = uiState.isShizukuInstalled,
                isRunningScript = uiState.isRunningShizukuScript,
                lastResult = uiState.lastScriptResult,
                logs = uiState.shizukuLogs,
                onRequestPermission = { viewModel.requestShizukuPermission() },
                onRefresh = { viewModel.refreshShizukuState() },
                onDownloadShizuku = { viewModel.openShizukuDownloadPage(context) },
                onOpenWirelessDebugging = { viewModel.openWirelessDebuggingSettings(context) }
            )
        }
        if (uiState.simSlots.isNotEmpty()) {
            item {
                MinimalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Kích nhanh IMS (persistent)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                        Text("Không mất sau reboot – nên làm trước.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        uiState.simSlots.forEach { sim ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${sim.carrierName} • SIM ${sim.slotIndex + 1}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Text)
                                    Text("subId=${sim.subscriptionId}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                PrimaryButton(text = "Bật", onClick = { viewModel.runImsProvisioningEnable(sim.subscriptionId) }, enabled = isReady && sim.subscriptionId != -1)
                            }
                            HorizontalDivider(color = Border)
                        }
                    }
                }
            }
        }
        items(scripts) { script ->
            ShizukuScriptCard(script = script, isReady = isReady, isRunning = uiState.isRunningShizukuScript, onRun = { viewModel.runShizukuScript(it) }, onCopy = { viewModel.copyToClipboard(context, it, "Script") })
        }
        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
private fun OtherTab(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    viewModel: VolteCheckerViewModel
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(CardGap)
    ) {
        item {
            MinimalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Đăng ký VoLTE nhà mạng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Text)
                    Text("Miễn phí – soạn SMS theo cú pháp bên dưới.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }
        items(uiState.carrierRegistrations) { carrier ->
            CarrierSmsCard(carrier = carrier, onSendSmsClick = { r, c -> viewModel.sendRegistrationSms(context, r, c) }, onCallUssdClick = { viewModel.dialCode(context, it) }, onCallHotlineClick = { viewModel.dialCode(context, it) })
        }
        item {
            TechnicalDetailsView(deviceInfo = uiState.deviceInfo, carrierConfig = uiState.carrierConfig)
        }
        item { Spacer(Modifier.height(64.dp)) }
    }
}
