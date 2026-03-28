package com.atruedev.kmpuwb.state

import com.atruedev.kmpuwb.error.SessionLost
import kotlin.test.Test
import kotlin.test.assertIs

class RangingStateTest {

    @Test
    fun idleReadyIsIdle() {
        val state: RangingState = RangingState.Idle.Ready
        assertIs<RangingState.Idle>(state)
    }

    @Test
    fun activeRangingIsActive() {
        val state: RangingState = RangingState.Active.Ranging
        assertIs<RangingState.Active>(state)
    }

    @Test
    fun stoppedByErrorCarriesError() {
        val error = SessionLost("connection lost")
        val state = RangingState.Stopped.ByError(error)

        assertIs<RangingState.Stopped>(state)
        assertIs<SessionLost>(state.error)
    }

    @Test
    fun stoppedByRequestIsTerminal() {
        val state: RangingState = RangingState.Stopped.ByRequest
        assertIs<RangingState.Stopped>(state)
    }

    @Test
    fun suspendedIsActive() {
        val state: RangingState = RangingState.Active.Suspended
        assertIs<RangingState.Active>(state)
    }

    @Test
    fun statesCanBeExhaustivelyMatched() {
        val state: RangingState = RangingState.Starting.Negotiating

        val description = when (state) {
            is RangingState.Idle -> "idle"
            is RangingState.Starting -> "starting"
            is RangingState.Active -> "active"
            is RangingState.Stopping -> "stopping"
            is RangingState.Stopped -> "stopped"
        }

        kotlin.test.assertEquals("starting", description)
    }
}
