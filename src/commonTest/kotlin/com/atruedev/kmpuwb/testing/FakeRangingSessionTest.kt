package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.error.SessionLost
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeRangingSessionTest {
    private val testPeer =
        Peer(
            address = PeerAddress(byteArrayOf(0x01, 0x02)),
            name = "test-tag",
        )

    @Test
    fun initialStateIsIdleReady() {
        val session = FakeRangingSession()
        assertIs<RangingState.Idle.Ready>(session.state.value)
    }

    @Test
    fun startTransitionsToActiveRanging() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            assertIs<RangingState.Active.Ranging>(session.state.value)
        }

    @Test
    fun startAddsPeerToActivePeers() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            assertTrue(session.activePeers.contains(testPeer))
        }

    @Test
    fun closeTransitionsToStoppedByRequest() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            session.close()
            assertIs<RangingState.Stopped.ByRequest>(session.state.value)
        }

    @Test
    fun closeClearsActivePeers() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            session.close()
            assertTrue(session.activePeers.isEmpty())
        }

    @Test
    fun simulatePeerLostRemovesPeer() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            session.simulatePeerLost(testPeer)
            assertTrue(session.activePeers.isEmpty())
            assertIs<RangingState.Active.PeerLost>(session.state.value)
        }

    @Test
    fun simulateErrorTransitionsToStoppedByError() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)

            val error = SessionLost("test error")
            session.simulateError(error)

            assertIs<RangingState.Stopped.ByError>(session.state.value)
            assertEquals(error, (session.state.value as RangingState.Stopped.ByError).error)
        }

    @Test
    fun simulateSuspendTransitionsToSuspended() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            session.simulateSuspend()
            assertIs<RangingState.Active.Suspended>(session.state.value)
        }

    @Test
    fun simulateResumeTransitionsToRanging() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            session.simulateSuspend()
            session.simulateResume()
            assertIs<RangingState.Active.Ranging>(session.state.value)
        }

    @Test
    fun simulatePeerDisconnectTransitionsToStoppedByPeer() =
        runTest {
            val session = FakeRangingSession()
            session.start(testPeer)
            session.simulatePeerDisconnect()
            assertIs<RangingState.Stopped.ByPeer>(session.state.value)
        }
}
