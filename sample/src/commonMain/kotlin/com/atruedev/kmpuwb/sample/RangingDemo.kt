package com.atruedev.kmpuwb.sample

import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.adapter.UwbAdapterState
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.ranging.Angle
import com.atruedev.kmpuwb.ranging.Distance
import com.atruedev.kmpuwb.ranging.RangingMeasurement
import com.atruedev.kmpuwb.session.RangingResult
import com.atruedev.kmpuwb.session.RangingSession
import com.atruedev.kmpuwb.state.RangingState
import com.atruedev.kmpuwb.testing.FakeRangingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RangingDemo(
    private val scope: CoroutineScope,
) {
    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private var session: RangingSession? = null
    private var rangingJob: Job? = null

    fun start() {
        scope.launch {
            try {
                val adapter = UwbAdapter()
                appendLog("UWB adapter created")
                logAdapterState(adapter)
                logCapabilities(adapter)
                startDemo(adapter)
            } catch (e: Exception) {
                appendLog("UWB init failed: ${e.message}")
                appendLog("Falling back to simulated ranging")
                startSimulatedDemo()
            }
        }
    }

    fun stop() {
        rangingJob?.cancel()
        session?.close()
        session = null
        scope.cancel()
    }

    private fun logAdapterState(adapter: UwbAdapter) {
        scope.launch {
            adapter.state.collect { state ->
                when (state) {
                    UwbAdapterState.ON -> appendLog("UWB adapter: ready")
                    UwbAdapterState.OFF -> appendLog("UWB adapter: disabled")
                    UwbAdapterState.UNSUPPORTED -> appendLog("UWB adapter: no hardware")
                }
            }
        }
    }

    private suspend fun logCapabilities(adapter: UwbAdapter) {
        val caps = adapter.capabilities()
        appendLog(
            "Capabilities: roles=${caps.supportedRoles}, " +
                "AoA=${caps.angleOfArrivalSupported}, " +
                "channels=${caps.supportedChannels}",
        )
    }

    private suspend fun startDemo(adapter: UwbAdapter) {
        if (adapter.state.value != UwbAdapterState.ON) {
            appendLog("No UWB hardware — using simulated ranging")
            startSimulatedDemo()
            return
        }

        appendLog("UWB hardware detected — no remote peer available")
        appendLog("Falling back to simulated ranging\n")
        startSimulatedDemo()
    }

    private suspend fun startSimulatedDemo() {
        appendLog("--- Simulated Ranging Session ---\n")

        val fakeSession = FakeRangingSession()
        session = fakeSession

        val peer =
            Peer(
                address = PeerAddress(byteArrayOf(0xA1.toByte(), 0xB2.toByte())),
                name = "UWB-Tag-001",
            )

        fakeSession.start(peer)
        observeSession(fakeSession)

        scope.launch {
            simulateApproach(fakeSession, peer)
        }
    }

    private fun observeSession(session: RangingSession) {
        scope.launch {
            session.state.collect { state ->
                appendLog("State: ${formatState(state)}")
            }
        }

        rangingJob =
            scope.launch {
                session.rangingResults.collect { result ->
                    logResult(result)
                }
            }
    }

    private suspend fun simulateApproach(
        session: FakeRangingSession,
        peer: Peer,
    ) {
        delay(500)
        appendLog("\n>> Peer approaching...")

        var distance = 5.0
        var azimuth = -30.0
        while (distance > 0.5) {
            session.emitResult(
                RangingResult.Position(
                    peer = peer,
                    measurement =
                        RangingMeasurement(
                            distance = Distance.meters(distance),
                            azimuth = Angle.degrees(azimuth),
                            elevation = Angle.degrees(-2.0),
                        ),
                ),
            )
            distance -= 0.4
            azimuth += 5.0
            delay(400)
        }

        appendLog("\n>> Peer moving out of range...")
        delay(600)
        session.simulatePeerLost(peer)
        delay(1000)

        appendLog("\n>> Peer returning...")
        session.simulatePeerRecovered(
            RangingResult.PeerRecovered(
                peer = peer,
                measurement =
                    RangingMeasurement(
                        distance = Distance.meters(2.0),
                        azimuth = Angle.degrees(10.0),
                        elevation = Angle.degrees(0.0),
                    ),
            ),
        )
        delay(1000)

        appendLog("\n>> Session suspended (app backgrounded)...")
        session.simulateSuspend()
        delay(1500)

        appendLog(">> Session resumed")
        session.simulateResume()
        delay(500)

        session.emitResult(
            RangingResult.Position(
                peer = peer,
                measurement =
                    RangingMeasurement(
                        distance = Distance.meters(1.2),
                        azimuth = Angle.degrees(5.0),
                        elevation = Angle.degrees(1.0),
                    ),
            ),
        )
        delay(1000)

        appendLog("\n--- Demo complete ---")
    }

    private fun logResult(result: RangingResult) {
        when (result) {
            is RangingResult.Position -> {
                val m = result.measurement
                val name = result.peer.name ?: result.peer.address.toString()
                appendLog(
                    "  $name: ${m.distance} " +
                        "az=${m.azimuth ?: "n/a"} " +
                        "el=${m.elevation ?: "n/a"}",
                )
            }
            is RangingResult.PeerLost ->
                appendLog("  Peer lost: ${result.peer.name ?: result.peer.address}")
            is RangingResult.PeerRecovered -> {
                val m = result.measurement
                appendLog(
                    "  Peer recovered: ${result.peer.name ?: result.peer.address} " +
                        "at ${m.distance}",
                )
            }
        }
    }

    private fun formatState(state: RangingState): String =
        when (state) {
            is RangingState.Idle.Ready -> "Idle (ready)"
            is RangingState.Idle.Unsupported -> "Idle (unsupported)"
            is RangingState.Starting.Negotiating -> "Starting (negotiating)"
            is RangingState.Starting.Initializing -> "Starting (initializing)"
            is RangingState.Active.Ranging -> "Active (ranging)"
            is RangingState.Active.Suspended -> "Active (suspended)"
            is RangingState.Active.PeerLost -> "Active (peer lost)"
            is RangingState.Stopped.ByRequest -> "Stopped (by request)"
            is RangingState.Stopped.ByPeer -> "Stopped (by peer)"
            is RangingState.Stopped.ByError -> "Stopped (error: ${state.error.message})"
            is RangingState.Stopped.BySystemEvent -> "Stopped (system event)"
        }

    private fun appendLog(message: String) {
        _log.value += "$message\n"
    }
}
