@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.NearbyInteraction.NISession
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

private val NI_SESSION_INIT_TIMEOUT = 5.seconds

internal class IosPreparedSession private constructor(
    override val config: RangingConfig,
    private val niSession: NISession,
    override val localParams: SessionParams,
) : PreparedSession {
    private val consumed = kotlin.concurrent.AtomicInt(0)

    override suspend fun startRanging(remoteParams: SessionParams): RangingSession {
        check(consumed.compareAndSet(0, 1)) { "PreparedSession has already been consumed" }

        val rangingSession = IosRangingSession(config, existingSession = niSession)
        rangingSession.startRanging(remoteParams)
        return rangingSession
    }

    override fun close() {
        if (consumed.compareAndSet(0, 1)) {
            dispatch_async(dispatch_get_main_queue()) {
                niSession.invalidate()
            }
        }
    }

    companion object {
        suspend fun create(config: RangingConfig): IosPreparedSession {
            val (session, token) =
                withTimeout(NI_SESSION_INIT_TIMEOUT) {
                    suspendCancellableCoroutine { cont ->
                        dispatch_async(dispatch_get_main_queue()) {
                            val niSession = NISession()
                            cont.invokeOnCancellation { niSession.invalidate() }
                            val token = niSession.discoveryToken
                            if (token != null) {
                                cont.resume(niSession to token)
                            } else {
                                niSession.invalidate()
                                cont.resumeWithException(
                                    IllegalStateException(
                                        "NISession.discoveryToken unavailable — " +
                                            "device may not support NearbyInteraction",
                                    ),
                                )
                            }
                        }
                    }
                }

            return IosPreparedSession(
                config = config,
                niSession = session,
                localParams = SessionParams(serializeDiscoveryToken(token)),
            )
        }
    }
}
