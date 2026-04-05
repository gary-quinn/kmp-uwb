package com.atruedev.kmpuwb.adapter

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class UwbAdapterInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val hasUwb: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_UWB)

    @Test
    fun adapterFactoryReturnsNonNull() {
        val adapter = UwbAdapter()
        assertNotNull(adapter)
        adapter.close()
    }

    @Test
    fun adapterReportsUnsupportedOnNonUwbDevice() {
        Assume.assumeFalse("Device has UWB — skipping UNSUPPORTED test", hasUwb)
        val adapter = UwbAdapter()
        assertEquals(UwbAdapterState.UNSUPPORTED, adapter.state.value)
        adapter.close()
    }

    @Test
    fun capabilitiesReturnNoneOnNonUwbDevice() =
        runTest {
            Assume.assumeFalse("Device has UWB — skipping NONE capabilities test", hasUwb)
            val adapter = UwbAdapter()
            assertEquals(UwbCapabilities.NONE, adapter.capabilities())
            adapter.close()
        }

    @Test
    fun adapterReportsOnWhenUwbAvailable() {
        Assume.assumeTrue("UWB hardware required", hasUwb)
        val adapter = UwbAdapter()
        assertEquals(UwbAdapterState.ON, adapter.state.value)
        adapter.close()
    }

    @Test
    fun capabilitiesNonEmptyOnUwbDevice() =
        runTest {
            Assume.assumeTrue("UWB hardware required", hasUwb)
            val adapter = UwbAdapter()
            val capabilities = adapter.capabilities()
            assertTrue(capabilities.supportedChannels.isNotEmpty())
            adapter.close()
        }
}
