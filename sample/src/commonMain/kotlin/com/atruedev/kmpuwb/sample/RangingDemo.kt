package com.atruedev.kmpuwb.sample

import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.adapter.UwbAdapterState
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.config.StsMode
import com.atruedev.kmpuwb.config.rangingConfig
import com.atruedev.kmpuwb.connector.ConnectorException
import com.atruedev.kmpuwb.connector.PeerConnector
import com.atruedev.kmpuwb.connector.startWithConnector
import com.atruedev.kmpuwb.session.RangingResult
import com.atruedev.kmpuwb.session.RangingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Demonstrates the full kmp-uwb API surface:
 * adapter state -> capabilities -> prepareSession -> OOB exchange -> ranging.
 *
 * Platform entry points (Android Activity, iOS ViewController) create a
 * [RangingDemo] with a scope and a [PeerConnector] implementation that
 * handles the OOB transport (BLE, NFC, etc.).
 */
class RangingDemo(
    private val scope: CoroutineScope,
    private val connector: PeerConnector,
    private val log: (String) -> Unit,
) {
    private val adapter = UwbAdapter()
    private var session: RangingSession? = null
    private var rangingJob: Job? = null

    fun start() {
        observeAdapterState()
        scope.launch { checkCapabilitiesAndRange() }
    }

    fun stop() {
        rangingJob?.cancel()
        session?.close()
        session = null
    }

    private fun observeAdapterState() {
        adapter.state.onEach { state ->
            when (state) {
                UwbAdapterState.ON -> log("UWB adapter: ready")
                UwbAdapterState.OFF -> log("UWB adapter: disabled")
                UwbAdapterState.UNSUPPORTED -> log("UWB adapter: no hardware")
            }
        }.launchIn(scope)
    }

    private suspend fun checkCapabilitiesAndRange() {
        if (adapter.state.value != UwbAdapterState.ON) {
            log("UWB not available — skipping ranging")
            return
        }

        val capabilities = adapter.capabilities()
        log(
            "Capabilities: roles=${capabilities.supportedRoles}, " +
                "AoA=${capabilities.angleOfArrivalSupported}, " +
                "channels=${capabilities.supportedChannels}",
        )

        startRanging()
    }

    private suspend fun startRanging() {
        val config =
            rangingConfig {
                role = RangingRole.CONTROLLER
                channel = 9
                stsMode = StsMode.DYNAMIC
                angleOfArrival = true
            }

        try {
            log("Preparing session...")
            session = adapter.startWithConnector(config, connector)
            log("Ranging started")
            observeSession()
        } catch (e: ConnectorException) {
            log("OOB exchange failed: ${e.error.message}")
        }
    }

    private fun observeSession() {
        val currentSession = session ?: return

        currentSession.state
            .onEach { state -> log("State: $state") }
            .launchIn(scope)

        rangingJob =
            currentSession.rangingResults
                .onEach { result -> logResult(result) }
                .launchIn(scope)
    }

    private fun logResult(result: RangingResult) {
        when (result) {
            is RangingResult.Position -> {
                val m = result.measurement
                log(
                    "Position: ${m.distance} " +
                        "azimuth=${m.azimuth ?: "n/a"} " +
                        "elevation=${m.elevation ?: "n/a"}",
                )
            }
            is RangingResult.PeerLost ->
                log("Peer lost: ${result.peer.address}")
            is RangingResult.PeerRecovered ->
                log("Peer recovered: ${result.peer.address} at ${result.measurement.distance}")
        }
    }
}
