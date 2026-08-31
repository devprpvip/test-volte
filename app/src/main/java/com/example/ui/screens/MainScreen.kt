package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsCell
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StatusSuccess
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
import com.example.ui.components.VerdictHeroCard
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.VoLtePrimary
import com.example.ui.theme.VoLteSecondary
import com.example.ui.viewmodel.VolteCheckerViewModel

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
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    val tabTitles = listOf(
        "Chẩn đoán" to Icons.Default.PhoneInTalk,
        "Band & Quốc gia" to Icons.Default.SignalCellularAlt,
        "Mã ẩn & Mở khóa" to Icons.Default.Code,
        "Cẩm nang Hãng" to Icons.Default.Smartphone,
        "Đăng ký Nhà mạng" to Icons.Default.CellTower,
        "Chi tiết IMS" to Icons.Default.Tune,
        "Shizuku ⭐" to Icons.Default.Security
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VoLtePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VoLTE Checker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kiểm tra & Mở khóa VoLTE ẩn",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Nút quay lại Lite (fork siêu đơn giản)
                    IconButton(
                        onClick = { viewModel.setSimpleMode(true) },
                        modifier = Modifier.testTag("switch_to_lite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Về bản Lite",
                            tint = VoLtePrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadInitialData() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = VoLtePrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openMobileNetworkSettings(context) },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cài đặt mạng",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openTestingRadioInfo(context) },
                containerColor = VoLtePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("fab_radio_info")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Dialpad,
                        contentDescription = "Mở Radio Info",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Menu ẩn 4636", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Shortcuts Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickShortcutChip(
                    icon = Icons.Default.Dialpad,
                    label = "Menu ẩn *#*#4636#*#*",
                    onClick = { viewModel.openTestingRadioInfo(context) }
                )
                QuickShortcutChip(
                    icon = Icons.Default.SettingsCell,
                    label = "Cài đặt Mạng di động",
                    onClick = { viewModel.openMobileNetworkSettings(context) }
                )
                QuickShortcutChip(
                    icon = Icons.Default.Tune,
                    label = "Cài đặt APN",
                    onClick = { viewModel.openApnSettings(context) }
                )
                QuickShortcutChip(
                    icon = Icons.Default.SignalCellularAlt,
                    label = "Band",
                    onClick = { viewModel.setTab(1) }
                )
                QuickShortcutChip(
                    icon = Icons.Default.Security,
                    label = "Shizuku",
                    onClick = { viewModel.setTab(6) }
                )
            }

            // Permission Request Notice if needed
            if (!uiState.hasPermission) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StatusWarning.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cấp quyền Điện thoại để đọc chi tiết SIM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Giúp phát hiện chính xác cấu hình VoLTE/IMS của từng thẻ SIM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_PHONE_STATE) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Cấp quyền", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = VoLtePrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (uiState.selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                            color = VoLtePrimary,
                            height = 3.dp
                        )
                    }
                }
            ) {
                tabTitles.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.setTab(index) },
                        modifier = Modifier.testTag("tab_$index"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            // Content Area
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VoLtePrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Đang quét thông số modem & cấu hình VoLTE...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> DiagnosticsTabContent(
                        uiState = uiState,
                        onQuickFixClick = { viewModel.setTab(2) }
                    )
                    1 -> BandTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    2 -> SecretCodesTabContent(
                        uiState = uiState,
                        onDialClick = { code -> viewModel.dialCode(context, code) },
                        onCopyClick = { code -> viewModel.copyToClipboard(context, code) },
                        onFilterChange = { filter -> viewModel.setBrandFilter(filter) }
                    )
                    3 -> BrandGuidesTabContent(
                        uiState = uiState,
                        onDialSecretCode = { code -> viewModel.dialCode(context, code) }
                    )
                    4 -> CarrierRegistrationsTabContent(
                        uiState = uiState,
                        onSendSmsClick = { recipient, command -> viewModel.sendRegistrationSms(context, recipient, command) },
                        onCallUssdClick = { ussd -> viewModel.dialCode(context, ussd) },
                        onCallHotlineClick = { hotline -> viewModel.dialCode(context, hotline) }
                    )
                    5 -> TechnicalDetailsTabContent(
                        uiState = uiState
                    )
                    6 -> ShizukuTabContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VoLtePrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DiagnosticsTabContent(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    onQuickFixClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("diagnostics_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Verdict Card
        item {
            VerdictHeroCard(
                verdict = uiState.verdict,
                onQuickFixClick = onQuickFixClick
            )
        }

        // SIM Slots Section
        if (uiState.simSlots.isNotEmpty()) {
            item {
                Text(
                    text = "TRẠNG THÁI KHE SIM & MẠNG DI ĐỘNG",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(uiState.simSlots) { simInfo ->
                SimSlotCard(simInfo = simInfo)
            }
        }

        // The 3 Direct Detailed Questions
        item {
            QuestionDetailCards(verdict = uiState.verdict)
        }

        // Call Quality & Testing Guide Notice
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = VoLtePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cách nhận biết VoLTE đang hoạt động thực tế:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. Thanh trạng thái hiện biểu tượng 'VoLTE' hoặc chữ 'HD' cạnh cột sóng 4G.\n2. Khi gọi điện thoại, mạng vẫn giữ nguyên ở '4G' hoặc '5G' (KHÔNG bị tụt xuống 3G/2G/H+).\n3. Bạn có thể vừa gọi điện thoại vừa lướt web tốc độ cao cùng lúc.\n4. Thời gian kết nối cuộc gọi siêu nhanh chỉ trong 1 - 2 giây và âm thanh trong trẻo chuẩn HD Voice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun SecretCodesTabContent(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    onDialClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onFilterChange: (String) -> Unit
) {
    val brands = listOf("Tất cả", "Xiaomi", "Universal", "Samsung", "Oppo", "MediaTek")

    val filteredCodes = uiState.secretCodes.filter { codeItem ->
        if (uiState.activeBrandFilter == "Tất cả") true
        else codeItem.targetBrand.contains(uiState.activeBrandFilter, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("secret_codes_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Intro Notice
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MÃ BÍ MẬT MỞ KHÓA & MENU ẨN",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bấm 'Gõ bàn phím' để tự động nạp mã vào trình quay số của máy, hoặc bấm 'Sao chép' để dán thủ công.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Brand Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                brands.forEach { brand ->
                    FilterChip(
                        selected = uiState.activeBrandFilter == brand,
                        onClick = { onFilterChange(brand) },
                        label = { Text(brand) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoLtePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        items(filteredCodes) { codeItem ->
            SecretCodeCard(
                item = codeItem,
                onDialClick = onDialClick,
                onCopyClick = onCopyClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun BrandGuidesTabContent(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    onDialSecretCode: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("brand_guides_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "HƯỚNG DẪN MỞ KHÓA THEO TỪNG HÃNG",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mỗi nhà sản xuất (Xiaomi, Pixel, Samsung, Oppo, v.v.) có cơ chế quản lý VoLTE và cách làm hiện nút gạt khác nhau.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(uiState.brandGuides) { guide ->
            BrandGuideCard(
                guide = guide,
                onDialSecretCode = onDialSecretCode
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun CarrierRegistrationsTabContent(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    onSendSmsClick: (recipient: String, command: String) -> Unit,
    onCallUssdClick: (String) -> Unit,
    onCallHotlineClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("carrier_registrations_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CÚ PHÁP ĐĂNG KÝ VOLTE NHÀ MẠNG",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tất cả các nhà mạng tại Việt Nam đều miễn phí 100% cước đăng ký và duy trì dịch vụ VoLTE HD Call. Nhấn nút để tự động soạn tin nhắn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(uiState.carrierRegistrations) { carrier ->
            CarrierSmsCard(
                carrier = carrier,
                onSendSmsClick = onSendSmsClick,
                onCallUssdClick = onCallUssdClick,
                onCallHotlineClick = onCallHotlineClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun TechnicalDetailsTabContent(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("technical_details_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TechnicalDetailsView(
                deviceInfo = uiState.deviceInfo,
                carrierConfig = uiState.carrierConfig
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = VoLtePrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Liên hệ hỗ trợ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("Cần hỗ trợ VoLTE / Shizuku / band? Liên hệ tác giả:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = RoundedCornerShape(12.dp), color = VoLtePrimary.copy(alpha = 0.08f), border = androidx.compose.foundation.BorderStroke(1.dp, VoLtePrimary.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Hậu Minh — devprpvip", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Facebook: https://www.facebook.com/minhhau036", style = MaterialTheme.typography.bodySmall, color = VoLtePrimary)
                            Text("Email: minhhaulivetime@hotmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.facebook.com/minhhau036")).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mở Facebook", fontWeight = FontWeight.Bold)
                    }
                    Text("Apache 2.0 — Copyright 2026 devprpvip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun BandTabContent(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    viewModel: com.example.ui.viewmodel.VolteCheckerViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val result = uiState.bandResult
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("band_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = VoLtePrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("KIỂM TRA BAND & QUỐC GIA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("Xác định băng tần LTE/5G đang dùng và so khớp với nhà mạng tại quốc gia của bạn. Cần quyền Vị trí + Điện thoại để đọc band thực (nếu thiếu, hiển thị giả định B3 cho VN).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (result == null) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = VoLtePrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Đang đọc band & quốc gia…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Thiết bị & Hệ điều hành", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        BandRowOld(label = "Nhà sản xuất", value = result.device.manufacturer)
                        BandRowOld(label = "Model", value = result.device.model)
                        BandRowOld(label = "Android", value = result.device.androidVersion)
                        BandRowOld(label = "Thương hiệu", value = result.device.brand)
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Quốc gia & Nhà mạng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        BandRowOld(label = "Quốc gia", value = "${result.country.countryName} (${result.country.countryIso.uppercase()})")
                        BandRowOld(label = "MCC", value = result.country.mcc ?: "—")
                        BandRowOld(label = "Nhà mạng", value = result.country.operatorName ?: result.carrierMatch?.carrierName ?: "Chưa xác định")
                        BandRowOld(label = "Mạng hỗ trợ", value = result.carrierMatch?.let { it.bands.joinToString { b -> "B${b.band} (${b.frequency})" } } ?: "—")
                        BandRowOld(label = "VoLTE bands", value = result.carrierMatch?.volteBands?.joinToString { "B$it" } ?: "B3, B1, B8 (mặc định VN)")
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Băng tần hiện tại", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = when (result.supportLevel) {
                                    BandCheckManager.SupportLevel.FULL -> StatusSuccess.copy(alpha = 0.12f)
                                    BandCheckManager.SupportLevel.PARTIAL -> StatusWarning.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, when (result.supportLevel) {
                                    BandCheckManager.SupportLevel.FULL -> StatusSuccess.copy(alpha = 0.3f)
                                    BandCheckManager.SupportLevel.PARTIAL -> StatusWarning.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                })
                            ) {
                                Text(
                                    text = when (result.supportLevel) {
                                        BandCheckManager.SupportLevel.FULL -> "Tương thích"
                                        BandCheckManager.SupportLevel.PARTIAL -> "Hạn chế"
                                        BandCheckManager.SupportLevel.UNKNOWN -> "Chưa rõ"
                                        BandCheckManager.SupportLevel.NO_BAND -> "Không xác định"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (result.supportLevel) {
                                        BandCheckManager.SupportLevel.FULL -> StatusSuccess
                                        BandCheckManager.SupportLevel.PARTIAL -> StatusWarning
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        BandRowOld(label = "Band", value = result.band.bandLabel)
                        BandRowOld(label = "EARFCN/NRARFCN", value = result.band.earfcn?.toString() ?: result.band.nrarfcn?.toString() ?: result.band.channelNumber?.toString() ?: "—")
                        BandRowOld(label = "RAT", value = result.band.ratType)
                        BandRowOld(label = "Đọc thực?", value = if (result.band.isDetected) "Có (từ modem)" else "Giả định (cần quyền Vị trí)")
                        if (!result.band.isDetected) {
                            Surface(shape = RoundedCornerShape(12.dp), color = StatusWarning.copy(alpha = 0.12f), border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                                Text("Cấp quyền Vị trí chính xác + Điện thoại để đọc band thực qua CellInfo. Hiện tại hiển thị B3 giả định cho VN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Hướng fix cho ${result.device.manufacturer} ${result.device.model}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = VoLtePrimary)
                        Text(result.fixGuide?.summary ?: "Áp dụng hướng chung.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        result.fixGuide?.steps?.forEachIndexed { idx, step ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(24.dp).background(VoLtePrimary.copy(alpha = 0.12f), RoundedCornerShape(999.dp)), contentAlignment = Alignment.Center) {
                                    Text("${idx + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = VoLtePrimary)
                                }
                                Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            result.fixGuide?.secretCode?.let { code ->
                                OutlinedButton(onClick = { viewModel.dialCode(context, code) }, shape = RoundedCornerShape(20.dp)) { Text(code, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                            result.fixGuide?.shizukuScriptId?.let {
                                Button(onClick = { viewModel.setTab(6) }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary)) { Text("Mở Shizuku", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                        }
                        result.fixGuide?.warning?.let { w ->
                            Surface(shape = RoundedCornerShape(12.dp), color = StatusWarning.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                                Text("⚠ $w", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun BandRowOld(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.2f))
    }
}

@Composable
private fun ShizukuTabContent(
    uiState: com.example.ui.viewmodel.VolteCheckerUiState,
    viewModel: com.example.ui.viewmodel.VolteCheckerViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isReady = uiState.shizukuState is ShizukuManager.ShizukuState.ReadyShell || uiState.shizukuState is ShizukuManager.ShizukuState.ReadyRoot
    val subIds = uiState.simSlots.mapNotNull { if (it.subscriptionId != -1) it.subscriptionId else null }.ifEmpty { listOf(0) }
    val scripts = rememberShizukuScripts(subIds)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shizuku_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Quick per-SIM provisioning buttons (persistent)
        if (uiState.simSlots.isNotEmpty()) {
            item {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = VoLtePrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("KÍCH NHANH IMS PROVISIONING (PERSISTENT)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text("Bật VoLTE provisioning persistent cho từng SIM - không mất sau reboot (iKirby method). Nên làm trước khi override carrier config.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        uiState.simSlots.forEach { sim ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${sim.carrierName} • SIM ${sim.slotIndex + 1}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("subId=${sim.subscriptionId} • ${sim.networkType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.runImsProvisioningEnable(sim.subscriptionId) },
                                    enabled = isReady && !uiState.isRunningShizukuScript && sim.subscriptionId != -1,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Bật VoLTE", fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                        if (isReady) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { subIds.forEach { viewModel.runBinderOverrideCarrierConfig(it, false) } }, enabled = !uiState.isRunningShizukuScript, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                    Text("Override CC non-persistent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(onClick = { subIds.forEach { viewModel.runBinderOverrideCarrierConfig(it, false) }; subIds.forEach { viewModel.runImsProvisioningEnable(it) } }, enabled = !uiState.isRunningShizukuScript, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = VoLtePrimary), modifier = Modifier.weight(1f)) {
                                    Text("Full Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        items(scripts) { script ->
            ShizukuScriptCard(
                script = script,
                isReady = isReady,
                isRunning = uiState.isRunningShizukuScript,
                onRun = { viewModel.runShizukuScript(it) },
                onCopy = { viewModel.copyToClipboard(context, it, "Script") }
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun rememberShizukuScripts(subIds: List<Int>): List<VolteActivationScripts.ScriptItem> {
    return androidx.compose.runtime.remember(subIds) { VolteActivationScripts.allScripts(subIds) }
}
