@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import platform.NearbyInteraction.NISession

internal class IosPreparedSession(
    override val config: RangingConfig,
) : PreparedSession {
    private val niSession = NISession()

    private var consumed: Boolean = false

    override val localParams: SessionParams by lazy {
        val token =
            niSession.discoveryToken
                ?: error("NISession.discoveryToken unavailable — device may not support UWB")
        SessionParams(serializeDiscoveryToken(token))
    }

    override suspend fun startRanging(remoteParams: SessionParams): RangingSession {
        check(!consumed) { "PreparedSession has already been consumed" }
        consumed = true

        val session = IosRangingSession(config, existingSession = niSession)
        session.startPrepared(remoteParams)
        return session
    }

    override fun close() {
        if (!consumed) {
            consumed = true
            niSession.invalidate()
        }
    }
}
