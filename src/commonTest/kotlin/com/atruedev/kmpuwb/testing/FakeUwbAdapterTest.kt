package com.atruedev.kmpuwb.testing

import app.cash.turbine.test
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
    fun stateTransitionSequence() =
        runTest {
            val adapter = FakeUwbAdapter()

            adapter.state.test {
                assertEquals(UwbAdapterState.ON, awaitItem())

                adapter.simulateDisabled()
                assertEquals(UwbAdapterState.OFF, awaitItem())

                adapter.simulateUnsupported()
                assertEquals(UwbAdapterState.UNSUPPORTED, awaitItem())

                adapter.simulateEnabled()
                assertEquals(UwbAdapterState.ON, awaitItem())
            }
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
    fun customInitialState() =
        runTest {
            val adapter = FakeUwbAdapter(initialState = UwbAdapterState.OFF)

            adapter.state.test {
                assertEquals(UwbAdapterState.OFF, awaitItem())
            }
        }
}
