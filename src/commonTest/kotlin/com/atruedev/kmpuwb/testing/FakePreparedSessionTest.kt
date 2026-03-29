package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.session.SessionParams
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakePreparedSessionTest {
    private val remoteParams = SessionParams(byteArrayOf(0x0A, 0x0B))

    @Test
    fun startRangingSetsFlags() =
        runTest {
            val prepared = FakePreparedSession()
            assertFalse(prepared.startRangingCalled)

            prepared.startRanging(remoteParams)

            assertTrue(prepared.startRangingCalled)
            assertEquals(remoteParams, prepared.lastRemoteParams)
        }

    @Test
    fun startRangingReturnsActiveSession() =
        runTest {
            val prepared = FakePreparedSession()
            val session = prepared.startRanging(remoteParams)
            assertIs<RangingState.Active.Ranging>(session.state.value)
        }

    @Test
    fun doubleStartRangingThrows() =
        runTest {
            val prepared = FakePreparedSession()
            prepared.startRanging(remoteParams)

            assertFailsWith<IllegalStateException> {
                prepared.startRanging(remoteParams)
            }
        }

    @Test
    fun closeSetsFlag() {
        val prepared = FakePreparedSession()
        assertFalse(prepared.closeCalled)
        prepared.close()
        assertTrue(prepared.closeCalled)
    }

    @Test
    fun startRangingAfterCloseThrows() =
        runTest {
            val prepared = FakePreparedSession()
            prepared.close()

            assertFailsWith<IllegalStateException> {
                prepared.startRanging(remoteParams)
            }
        }

    @Test
    fun localParamsHasDefaultValue() {
        val prepared = FakePreparedSession()
        assertEquals(FakePreparedSession.DEFAULT_LOCAL_PARAMS, prepared.localParams)
    }
}
