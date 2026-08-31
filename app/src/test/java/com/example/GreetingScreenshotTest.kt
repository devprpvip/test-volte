package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.ActiveStatus
import com.example.data.model.SupportStatus
import com.example.data.model.VisibilityStatus
import com.example.data.model.VolteVerdict
import com.example.ui.components.VerdictHeroCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun volte_hero_screenshot() {
        val sampleVerdict = VolteVerdict(
            deviceSupported = SupportStatus.SUPPORTED,
            deviceSupportReason = "Thiết bị hỗ trợ đầy đủ modem LTE và chuẩn IMS.",
            isVolteEnabled = ActiveStatus.ACTIVE_REGISTERED,
            enabledStatusReason = "VoLTE đang hoạt động bình thường.",
            settingsVisibility = VisibilityStatus.VISIBLE,
            visibilityReason = "Tùy chọn hiển thị trong Cài đặt.",
            overallSummary = "Máy bạn có hỗ trợ VoLTE và dịch vụ đang BẬT hoạt động bình thường!"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                VerdictHeroCard(
                    verdict = sampleVerdict,
                    onQuickFixClick = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
