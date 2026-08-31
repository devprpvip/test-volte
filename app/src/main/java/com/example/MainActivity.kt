package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.shizuku.ShizukuManager
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SimpleModeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VolteCheckerViewModel
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MainActivity : ComponentActivity() {

    private val viewModel: VolteCheckerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hidden API bypass for Android P+ (needed for ITelephony reflection via Shizuku)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try { HiddenApiBypass.addHiddenApiExemptions("L") } catch (_: Throwable) {}
        }
        // Init Shizuku early - Sui init + binder listeners
        try { ShizukuManager.init(this) } catch (_: Throwable) {}

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    if (uiState.isSimpleMode) {
                        SimpleModeScreen(
                            viewModel = viewModel,
                            onSwitchToClassic = { viewModel.setSimpleMode(false) }
                        )
                    } else {
                        // Classic 7-tab mode - giữ nguyên bản gốc, thêm nút quay lại Lite ở TopBar actions
                        MainScreen(viewModel = viewModel)
                        // Nút Lite được xử lý trong MainScreen via viewModel callback; nếu cần overlay:
                        // (không cần thêm gì ở đây, MainScreen đã có toggle qua viewModel)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not destroy Shizuku listeners here - ViewModel keeps observing
    }
}
