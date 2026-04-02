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
 * Created in [RangingState.Active.Ranging] by default — matching real sessions
 * returned by [com.atruedev.kmpuwb.session.PreparedSession.startRanging].
 *
 * ```
 * val session = FakeRangingSession()
 * session.emitResult(RangingResult.Position(peer, measurement))
 * session.simulatePeerLost(peer)
 * session.simulateError(SessionLost("connection lost"))
 * ```
 */
public class FakeRangingSession(
    override val config: RangingConfig = DEFAULT_CONFIG,
    initialState: RangingState = RangingState.Active.Ranging,
) : RangingSession {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<RangingState> = _state.asStateFlow()

    private val resultChannel = Channel<RangingResult>(capacity = Channel.UNLIMITED)
    override val rangingResults: Flow<RangingResult> = resultChannel.receiveAsFlow()

    private val _activePeers: MutableSet<Peer> = mutableSetOf()

    public val activePeers: Set<Peer> get() = _activePeers.toSet()

    override fun close() {
        if (_state.value !is RangingState.Stopped) {
            _state.value = RangingState.Stopped.ByRequest
        }
        _activePeers.clear()
        resultChannel.close()
    }

    public fun emitResult(result: RangingResult) {
        resultChannel.trySend(result)
    }

    public fun simulatePeerLost(peer: Peer) {
        _activePeers.remove(peer)
        resultChannel.trySend(RangingResult.PeerLost(peer))
        if (_activePeers.isEmpty()) {
            _state.value = RangingState.Active.PeerLost
        }
    }

    public fun simulatePeerRecovered(result: RangingResult.PeerRecovered) {
        _activePeers.add(result.peer)
        resultChannel.trySend(result)
        _state.value = RangingState.Active.Ranging
    }

    public fun simulateError(error: UwbError) {
        _state.value = RangingState.Stopped.ByError(error)
        _activePeers.clear()
        resultChannel.close()
    }

    public fun simulateSuspend() {
        _state.value = RangingState.Active.Suspended
    }

    public fun simulateResume() {
        _state.value = RangingState.Active.Ranging
    }

    public fun simulatePeerDisconnect() {
        _state.value = RangingState.Stopped.ByPeer
        _activePeers.clear()
        resultChannel.close()
    }

    public fun addPeer(peer: Peer) {
        _activePeers.add(peer)
    }

    public companion object {
        public val DEFAULT_CONFIG: RangingConfig =
            RangingConfig(
                role = RangingRole.CONTROLEE,
            )
    }
}
