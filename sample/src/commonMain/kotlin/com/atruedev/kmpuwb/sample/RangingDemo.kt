package com.atruedev.kmpuwb.sample

import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.adapter.UwbAdapterState
import com.atruedev.kmpuwb.config.RangingRole
import com.atruedev.kmpuwb.config.StsMode
import com.atruedev.kmpuwb.config.rangingConfig
import com.atruedev.kmpuwb.session.PreparedSession
import com.atruedev.kmpuwb.session.RangingResult
import com.atruedev.kmpuwb.session.RangingSession
import com.atruedev.kmpuwb.session.SessionParams
import com.atruedev.kmpuwb.state.RangingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

enum class DemoPhase {
    CHECKING_ADAPTER,
    WAITING_FOR_REMOTE_PARAMS,
    RANGING,
    ERROR,
}

private const val LOG_MAX_LINES = 200

@OptIn(ExperimentalEncodingApi::class)
class RangingDemo(
    private val scope: CoroutineScope,
    private val role: RangingRole,
) {
    private val logLines = mutableListOf<String>()
    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private val _phase = MutableStateFlow(DemoPhase.CHECKING_ADAPTER)
    val phase: StateFlow<DemoPhase> = _phase.asStateFlow()

    private val _localParamsBase64 = MutableStateFlow("")
    val localParamsBase64: StateFlow<String> = _localParamsBase64.asStateFlow()

    private var prepared: PreparedSession? = null
    private var session: RangingSession? = null
    private var activeJobs = mutableListOf<Job>()

    fun start() {
        launchTracked {
            try {
                val adapter = UwbAdapter()
                appendLog("UWB adapter created (role: ${role.name})")

                if (adapter.state.value != UwbAdapterState.ON) {
                    appendLog("UWB not available: ${adapter.state.value}")
                    _phase.value = DemoPhase.ERROR
                    return@launchTracked
                }

                val caps = adapter.capabilities()
                appendLog(
                    "Capabilities: roles=${caps.supportedRoles}, " +
                        "AoA=${caps.angleOfArrivalSupported}, " +
                        "channels=${caps.supportedChannels}",
                )

                prepareAndWait(adapter)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendLog("Error: ${e.message}")
                _phase.value = DemoPhase.ERROR
            }
        }
    }

    private suspend fun prepareAndWait(adapter: UwbAdapter) {
        val config =
            rangingConfig {
                this.role = this@RangingDemo.role
                channel = 9
                stsMode = StsMode.DYNAMIC
                angleOfArrival = true
            }

        appendLog("Preparing session...")
        val prep = adapter.prepareSession(config)
        prepared = prep

        val localBase64 = Base64.encode(prep.localParams.toByteArray())
        _localParamsBase64.value = localBase64
        appendLog("Local params ready (${prep.localParams.size} bytes)")
        appendLog("Waiting for remote params...\n")

        _phase.value = DemoPhase.WAITING_FOR_REMOTE_PARAMS
    }

    fun submitRemoteParams(base64: String) {
        launchTracked {
            try {
                val bytes = Base64.decode(base64.trim())
                val remoteParams = SessionParams(bytes)
                appendLog("Remote params received (${remoteParams.size} bytes)")

                val prep =
                    prepared ?: run {
                        appendLog("Error: session not prepared")
                        return@launchTracked
                    }

                appendLog("Starting ranging...")
                session = prep.startRanging(remoteParams)
                prepared = null

                _phase.value = DemoPhase.RANGING
                observeSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendLog("Failed to start ranging: ${e.message}")
                _phase.value = DemoPhase.ERROR
            }
        }
    }

    fun stop() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        session?.close()
        prepared?.close()
        session = null
        prepared = null
    }

    private fun observeSession() {
        val currentSession = session ?: return

        launchTracked {
            currentSession.state.collect { state ->
                appendLog("State: ${formatState(state)}")
            }
        }

        launchTracked {
            currentSession.rangingResults.collect { result ->
                logResult(result)
            }
        }
    }

    private fun launchTracked(block: suspend CoroutineScope.() -> Unit): Job {
        val job = scope.launch(block = block)
        activeJobs.add(job)
        job.invokeOnCompletion { activeJobs.remove(job) }
        return job
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
        logLines.add(message)
        if (logLines.size > LOG_MAX_LINES) {
            logLines.removeFirst()
        }
        _log.value = logLines.joinToString("\n")
    }
}
