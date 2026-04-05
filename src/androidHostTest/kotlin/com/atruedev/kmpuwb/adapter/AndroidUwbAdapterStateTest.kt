package com.atruedev.kmpuwb.adapter

import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AndroidUwbAdapterStateTest {
    @BeforeTest
    fun initContext() {
        KmpUwb.init(RuntimeEnvironment.getApplication())
    }

    @Test
    fun stateIsUnsupportedWhenFeatureUwbAbsent() {
        // Robolectric's default PackageManager does not include FEATURE_UWB,
        // so AndroidUwbAdapter should resolve to UNSUPPORTED.
        val adapter = UwbAdapter()
        assertEquals(UwbAdapterState.UNSUPPORTED, adapter.state.value)
    }

    @Test
    fun capabilitiesReturnsNoneWhenUnsupported() =
        runTest {
            val adapter = UwbAdapter()
            val capabilities = adapter.capabilities()
            assertEquals(UwbCapabilities.NONE, capabilities)
        }

    @Test
    fun factoryReturnsNonNullAdapter() {
        val adapter = UwbAdapter()
        // The adapter should be non-null and closeable without error
        adapter.close()
    }
}
