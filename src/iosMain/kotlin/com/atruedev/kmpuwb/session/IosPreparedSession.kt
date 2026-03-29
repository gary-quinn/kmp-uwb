@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.NearbyInteraction.NISession

internal class IosPreparedSession(
    override val config: RangingConfig,
) : PreparedSession {
    private val niSession = NISession()
    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
        )
    private var consumed: Boolean = false

    override val localParams: SessionParams by lazy {
        val token =
            niSession.discoveryToken
                ?: error("NISession.discoveryToken unavailable — device may not support UWB")
        SessionParams(serializeDiscoveryToken(token))
    }

    override suspend fun startRanging(remoteParams: SessionParams): RangingSession =
        withContext(scope.coroutineContext) {
            check(!consumed) { "PreparedSession has already been consumed" }
            consumed = true

            val session = IosRangingSession(config, existingSession = niSession)
            session.startPrepared(remoteParams)
            session
        }

    override fun close() {
        scope.launch {
            if (!consumed) {
                consumed = true
                niSession.invalidate()
            }
        }
    }
}
