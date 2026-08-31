package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.telephony.VolteDiagnosticManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VoLTE Checker", appName)
    }

    @Test
    fun `diagnostic manager returns hardware and secret codes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = VolteDiagnosticManager(context)

        val hwInfo = manager.getDeviceHardwareInfo()
        assertNotNull(hwInfo)

        val codes = VolteDiagnosticManager.getSecretCodes()
        assertTrue(codes.isNotEmpty())
        assertTrue(codes.any { it.code == "*#*#86583#*#*" })

        val carriers = VolteDiagnosticManager.getCarrierRegistrations()
        assertTrue(carriers.isNotEmpty())
        assertTrue(carriers.any { it.carrierKey == "viettel" })
    }
}
