package com.atruedev.kmpuwb.state

import com.atruedev.kmpuwb.error.ChipsetError
import com.atruedev.kmpuwb.error.SessionLost
import com.atruedev.kmpuwb.error.UwbUnavailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class RangingStateTest {
    @Test
    fun idleReadyIsIdle() {
        val state: RangingState = RangingState.Idle.Ready
        assertIs<RangingState.Idle>(state)
    }

    @Test
    fun idleUnsupportedIsIdle() {
        val state: RangingState = RangingState.Idle.Unsupported
        assertIs<RangingState.Idle>(state)
    }

    @Test
    fun startingNegotiatingIsStarting() {
        val state: RangingState = RangingState.Starting.Negotiating
        assertIs<RangingState.Starting>(state)
    }

    @Test
    fun startingInitializingIsStarting() {
        val state: RangingState = RangingState.Starting.Initializing
        assertIs<RangingState.Starting>(state)
    }

    @Test
    fun activeRangingIsActive() {
        val state: RangingState = RangingState.Active.Ranging
        assertIs<RangingState.Active>(state)
    }

    @Test
    fun activeSuspendedIsActive() {
        val state: RangingState = RangingState.Active.Suspended
        assertIs<RangingState.Active>(state)
    }

    @Test
    fun activePeerLostIsActive() {
        val state: RangingState = RangingState.Active.PeerLost
        assertIs<RangingState.Active>(state)
    }

    @Test
    fun stoppedByRequestIsTerminal() {
        val state: RangingState = RangingState.Stopped.ByRequest
        assertIs<RangingState.Stopped>(state)
    }

    @Test
    fun stoppedByPeerIsTerminal() {
        val state: RangingState = RangingState.Stopped.ByPeer
        assertIs<RangingState.Stopped>(state)
    }

    @Test
    fun stoppedBySystemEventIsTerminal() {
        val state: RangingState = RangingState.Stopped.BySystemEvent
        assertIs<RangingState.Stopped>(state)
    }

    @Test
    fun stoppedByErrorCarriesError() {
        val error = SessionLost("connection lost")
        val state = RangingState.Stopped.ByError(error)

        assertIs<RangingState.Stopped>(state)
        assertIs<SessionLost>(state.error)
        assertEquals("connection lost", state.error.message)
    }

    @Test
    fun stoppedByErrorWithDifferentErrorTypes() {
        val sessionError = RangingState.Stopped.ByError(SessionLost("lost"))
        val hardwareError = RangingState.Stopped.ByError(UwbUnavailable())
        val chipsetError = RangingState.Stopped.ByError(ChipsetError(42, "chipset fault"))

        assertNotEquals(sessionError, hardwareError)
        assertNotEquals(hardwareError, chipsetError)
    }

    @Test
    fun exhaustiveMatchCoversAllStates() {
        val allStates: List<RangingState> =
            listOf(
                RangingState.Idle.Ready,
                RangingState.Idle.Unsupported,
                RangingState.Starting.Negotiating,
                RangingState.Starting.Initializing,
                RangingState.Active.Ranging,
                RangingState.Active.Suspended,
                RangingState.Active.PeerLost,
                RangingState.Stopped.ByRequest,
                RangingState.Stopped.ByPeer,
                RangingState.Stopped.ByError(SessionLost("test")),
                RangingState.Stopped.BySystemEvent,
            )

        assertEquals(11, allStates.size)
        allStates.forEach { state ->
            when (state) {
                is RangingState.Idle -> {}
                is RangingState.Starting -> {}
                is RangingState.Active -> {}
                is RangingState.Stopped -> {}
            }
        }
    }
}
