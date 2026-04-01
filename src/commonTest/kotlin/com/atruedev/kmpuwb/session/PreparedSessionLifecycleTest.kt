package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.state.RangingState
import com.atruedev.kmpuwb.testing.FakePreparedSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PreparedSessionLifecycleTest {
    private val remoteParams = SessionParams(byteArrayOf(0x0A, 0x0B))

    @Test
    fun startRangingReturnsActiveSession() =
        runTest {
            val prepared = FakePreparedSession()
            val session = prepared.startRanging(remoteParams)
            assertIs<RangingState.Active.Ranging>(session.state.value)
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
    fun doubleStartRangingThrows() =
        runTest {
            val prepared = FakePreparedSession()
            prepared.startRanging(remoteParams)

            assertFailsWith<IllegalStateException> {
                prepared.startRanging(remoteParams)
            }
        }

    @Test
    fun closeIsIdempotent() {
        val prepared = FakePreparedSession()
        prepared.close()
        prepared.close()
        assertTrue(prepared.closeCalled)
    }

    @Test
    fun closeAfterStartRangingIsNoOp() =
        runTest {
            val prepared = FakePreparedSession()
            val session = prepared.startRanging(remoteParams)
            prepared.close()
            assertIs<RangingState.Active.Ranging>(session.state.value)
        }

    @Test
    fun sessionCloseTransitionsToStopped() =
        runTest {
            val prepared = FakePreparedSession()
            val session = prepared.startRanging(remoteParams)
            session.close()
            assertIs<RangingState.Stopped.ByRequest>(session.state.value)
        }
}
