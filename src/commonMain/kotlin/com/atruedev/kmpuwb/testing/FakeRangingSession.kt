package com.atruedev.kmpuwb.testing

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.error.UwbError
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.session.RangingResult
import com.atruedev.kmpuwb.session.RangingSession
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Test double for [RangingSession] that allows injecting measurements and simulating errors.
 *
 * ```
 * val session = FakeRangingSession()
 * session.start(peer)
 *
 * // Inject a measurement
 * session.emitResult(RangingResult.Position(peer, measurement))
 *
 * // Simulate peer loss
 * session.simulatePeerLost(peer)
 *
 * // Simulate session error
 * session.simulateError(SessionLost("connection lost"))
 * ```
 */
public class FakeRangingSession(
    override val config: RangingConfig = DEFAULT_CONFIG,
) : RangingSession {

    private val _state = MutableStateFlow<RangingState>(RangingState.Idle.Ready)
    override val state: StateFlow<RangingState> = _state.asStateFlow()

    private val resultChannel = Channel<RangingResult>(capacity = Channel.BUFFERED)
    override val rangingResults: Flow<RangingResult> = resultChannel.receiveAsFlow()

    /** Peers currently in range. */
    public val activePeers: MutableSet<Peer> = mutableSetOf()

    override suspend fun start(peer: Peer) {
        check(_state.value is RangingState.Idle.Ready) {
            "Cannot start session in state ${_state.value}"
        }
        _state.value = RangingState.Starting.Negotiating
        _state.value = RangingState.Starting.Initializing
        _state.value = RangingState.Active.Ranging
        activePeers.add(peer)
    }

    override fun close() {
        _state.value = RangingState.Stopped.ByRequest
        activePeers.clear()
        resultChannel.close()
    }

    /** Emit a ranging result to collectors. */
    public fun emitResult(result: RangingResult) {
        resultChannel.trySend(result)
    }

    /** Simulate a peer moving out of range. */
    public fun simulatePeerLost(peer: Peer) {
        activePeers.remove(peer)
        resultChannel.trySend(RangingResult.PeerLost(peer))
        if (activePeers.isEmpty()) {
            _state.value = RangingState.Active.PeerLost
        }
    }

    /** Simulate a peer returning to range with a new measurement. */
    public fun simulatePeerRecovered(result: RangingResult.PeerRecovered) {
        activePeers.add(result.peer)
        resultChannel.trySend(result)
        _state.value = RangingState.Active.Ranging
    }

    /** Simulate a session error. */
    public fun simulateError(error: UwbError) {
        _state.value = RangingState.Stopped.ByError(error)
        activePeers.clear()
        resultChannel.close()
    }

    /** Simulate the app being backgrounded. */
    public fun simulateSuspend() {
        _state.value = RangingState.Active.Suspended
    }

    /** Simulate the app returning to foreground. */
    public fun simulateResume() {
        _state.value = RangingState.Active.Ranging
    }

    /** Simulate the remote peer ending the session. */
    public fun simulatePeerDisconnect() {
        _state.value = RangingState.Stopped.ByPeer
        activePeers.clear()
        resultChannel.close()
    }

    public companion object {
        public val DEFAULT_CONFIG: RangingConfig = RangingConfig(
            role = RangingRole.CONTROLEE,
        )
    }
}
