package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.adapter.UwbAdapterState
import com.atruedev.kmpuwb.adapter.UwbCapabilities
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeUwbAdapterTest {
    @Test
    fun defaultStateIsOn() {
        val adapter = FakeUwbAdapter()
        assertEquals(UwbAdapterState.ON, adapter.state.value)
    }

    @Test
    fun simulateDisabledChangesState() {
        val adapter = FakeUwbAdapter()
        adapter.simulateDisabled()
        assertEquals(UwbAdapterState.OFF, adapter.state.value)
    }

    @Test
    fun simulateEnabledChangesState() {
        val adapter = FakeUwbAdapter()
        adapter.simulateDisabled()
        adapter.simulateEnabled()
        assertEquals(UwbAdapterState.ON, adapter.state.value)
    }

    @Test
    fun simulateUnsupportedChangesState() {
        val adapter = FakeUwbAdapter()
        adapter.simulateUnsupported()
        assertEquals(UwbAdapterState.UNSUPPORTED, adapter.state.value)
    }

    @Test
    fun capabilitiesReturnNoneWhenUnsupported() =
        runTest {
            val adapter = FakeUwbAdapter()
            adapter.simulateUnsupported()
            assertEquals(UwbCapabilities.NONE, adapter.capabilities())
        }

    @Test
    fun capabilitiesReturnDefaultWhenOn() =
        runTest {
            val adapter = FakeUwbAdapter()
            val capabilities = adapter.capabilities()
            assertTrue(capabilities.angleOfArrivalSupported)
            assertTrue(capabilities.supportedChannels.contains(9))
        }

    @Test
    fun customInitialState() {
        val adapter = FakeUwbAdapter(initialState = UwbAdapterState.OFF)
        assertEquals(UwbAdapterState.OFF, adapter.state.value)
    }
}
