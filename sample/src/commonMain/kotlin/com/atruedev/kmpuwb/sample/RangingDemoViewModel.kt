package com.atruedev.kmpuwb.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.atruedev.kmpuwb.session.SessionParams
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RangingDemoViewModel : ViewModel() {
    private val adapter = UwbAdapter()
    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private var session: RangingSession? = null
    private var rangingJob: Job? = null

    fun start() {
        observeAdapterState()
        viewModelScope.launch { checkCapabilitiesAndRange() }
    }

    fun stop() {
        rangingJob?.cancel()
        session?.close()
        session = null
    }

    private fun observeAdapterState() {
        viewModelScope.launch {
            adapter.state.collect { state ->
                when (state) {
                    UwbAdapterState.ON -> appendLog("UWB adapter: ready")
                    UwbAdapterState.OFF -> appendLog("UWB adapter: disabled")
                    UwbAdapterState.UNSUPPORTED -> appendLog("UWB adapter: no hardware")
                }
            }
        }
    }

    private suspend fun checkCapabilitiesAndRange() {
        if (adapter.state.value != UwbAdapterState.ON) {
            appendLog("UWB not available — skipping ranging")
            return
        }

        val caps = adapter.capabilities()
        appendLog(
            "Capabilities: roles=${caps.supportedRoles}, " +
                "AoA=${caps.angleOfArrivalSupported}, " +
                "channels=${caps.supportedChannels}",
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
            appendLog("Preparing session...")
            session = adapter.startWithConnector(config, stubConnector())
            appendLog("Ranging started")
            observeSession()
        } catch (e: ConnectorException) {
            appendLog("OOB exchange failed: ${e.error.message}")
        }
    }

    private fun observeSession() {
        val currentSession = session ?: return

        viewModelScope.launch {
            currentSession.state.collect { state ->
                appendLog("State: $state")
            }
        }

        rangingJob =
            viewModelScope.launch {
                currentSession.rangingResults.collect { result ->
                    logResult(result)
                }
            }
    }

    private fun logResult(result: RangingResult) {
        when (result) {
            is RangingResult.Position -> {
                val m = result.measurement
                appendLog(
                    "Position: ${m.distance} " +
                        "azimuth=${m.azimuth ?: "n/a"} " +
                        "elevation=${m.elevation ?: "n/a"}",
                )
            }
            is RangingResult.PeerLost ->
                appendLog("Peer lost: ${result.peer.address}")
            is RangingResult.PeerRecovered ->
                appendLog("Peer recovered: ${result.peer.address} at ${result.measurement.distance}")
        }
    }

    private fun appendLog(message: String) {
        _log.value += "$message\n"
    }
}

private fun stubConnector() =
    PeerConnector { localParams ->
        SessionParams(localParams.toByteArray())
    }
