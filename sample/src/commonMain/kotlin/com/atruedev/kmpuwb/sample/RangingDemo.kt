package com.atruedev.kmpuwb.sample

import com.atruedev.kmpble.scanner.Scanner
import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.adapter.UwbAdapterState
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.config.StsMode
import com.atruedev.kmpuwb.config.rangingConfig
import com.atruedev.kmpuwb.connector.ConnectorException
import com.atruedev.kmpuwb.connector.ble.BleConnector
import com.atruedev.kmpuwb.connector.ble.UwbOobService
import com.atruedev.kmpuwb.connector.startWithConnector
import com.atruedev.kmpuwb.session.RangingResult
import com.atruedev.kmpuwb.session.RangingSession
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

private const val LOG_MAX_LINES = 200

@OptIn(ExperimentalUuidApi::class)
class RangingDemo(
    private val scope: CoroutineScope,
    private val role: RangingRole,
) {
    private val logLines = mutableListOf<String>()
    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var rangingJob: Job? = null
    private var session: RangingSession? = null
    private var scanner: Scanner? = null

    fun start() {
        _error.value = null
        rangingJob =
            scope.launch {
                try {
                    val adapter = UwbAdapter()
                    appendLog("Role: ${role.name}")

                    if (adapter.state.value != UwbAdapterState.ON) {
                        _error.value = "UWB not available: ${adapter.state.value}"
                        return@launch
                    }

                    appendLog("UWB: ready")
                    logCapabilities(adapter)
                    startRanging(adapter)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _error.value = e.message ?: "Unknown error"
                }
            }
    }

    fun stop() {
        rangingJob?.cancel()
        rangingJob = null
        session?.close()
        scanner?.close()
        session = null
        scanner = null
    }

    private suspend fun logCapabilities(adapter: UwbAdapter) {
        val caps = adapter.capabilities()
        appendLog(
            "Capabilities: AoA=${caps.angleOfArrivalSupported}, " +
                "channels=${caps.supportedChannels}",
        )
    }

    private suspend fun startRanging(adapter: UwbAdapter) {
        val config =
            rangingConfig {
                this.role = this@RangingDemo.role
                channel = 9
                stsMode = StsMode.DYNAMIC
                angleOfArrival = true
            }

        val s =
            when (role) {
                RangingRole.CONTROLLER -> {
                    appendLog("Scanning for controlee...")
                    Scanner {
                        filters {
                            match { serviceUuid(UwbOobService.SERVICE_UUID) }
                        }
                    }
                }
                RangingRole.CONTROLEE -> null
            }

        scanner = s

        try {
            val connector =
                when (role) {
                    RangingRole.CONTROLLER -> BleConnector.controller(s!!)
                    RangingRole.CONTROLEE -> {
                        appendLog("Advertising, waiting for controller...")
                        BleConnector.controlee()
                    }
                }

            session = adapter.startWithConnector(config, connector)
            appendLog("Ranging started\n")
            observeSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ConnectorException) {
            _error.value = "BLE exchange failed: ${e.error.message}"
        } catch (e: Exception) {
            _error.value = "${e::class.simpleName}: ${e.message}"
        } finally {
            s?.close()
            scanner = null
        }
    }

    private suspend fun observeSession() {
        val currentSession = session ?: return

        coroutineScope {
            launch {
                currentSession.state.collect { state ->
                    appendLog("State: ${formatState(state)}")
                }
            }

            launch {
                currentSession.rangingResults.collect { result ->
                    logResult(result)
                }
            }
        }
    }

    private fun logResult(result: RangingResult) {
        when (result) {
            is RangingResult.Position -> {
                val m = result.measurement
                val name = result.peer.name ?: result.peer.address.toString()
                appendLog(
                    "$name: ${m.distance} " +
                        "az=${m.azimuth ?: "n/a"} " +
                        "el=${m.elevation ?: "n/a"}",
                )
            }
            is RangingResult.PeerLost ->
                appendLog("Peer lost: ${result.peer.name ?: result.peer.address}")
            is RangingResult.PeerRecovered ->
                appendLog("Peer recovered: ${result.peer.name ?: result.peer.address}")
        }
    }

    private fun formatState(state: RangingState): String =
        when (state) {
            is RangingState.Idle.Ready -> "Idle"
            is RangingState.Idle.Unsupported -> "Unsupported"
            is RangingState.Starting.Negotiating -> "Negotiating"
            is RangingState.Starting.Initializing -> "Initializing"
            is RangingState.Active.Ranging -> "Ranging"
            is RangingState.Active.Suspended -> "Suspended"
            is RangingState.Active.PeerLost -> "Peer lost"
            is RangingState.Stopped.ByRequest -> "Stopped"
            is RangingState.Stopped.ByPeer -> "Peer disconnected"
            is RangingState.Stopped.ByError -> "Error: ${state.error.message}"
            is RangingState.Stopped.BySystemEvent -> "System event"
        }

    private fun appendLog(message: String) {
        logLines.add(message)
        if (logLines.size > LOG_MAX_LINES) {
            logLines.removeFirst()
        }
        _log.value = logLines.joinToString("\n")
    }
}
