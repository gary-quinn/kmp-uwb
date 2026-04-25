package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.BackpressureStrategy
import com.atruedev.kmpuwb.config.RangingConfig

/**
 * A UWB session that has been prepared but not yet started.
 *
 * Holds allocated platform resources and exposes [localParams] for
 * out-of-band exchange with a remote peer. Once the remote peer's
 * parameters have been received, call [startRanging] to decode them
 * and begin the session.
 *
 * If ranging is not needed, call [close] to release platform resources.
 *
 * ```kotlin
 * val prepared = adapter.prepareSession(config)
 * val localBytes = prepared.localParams.toByteArray()
 * // ... exchange over BLE/NFC/WiFi ...
 * val session = prepared.startRanging(remoteParams)
 * ```
 */
public interface PreparedSession : AutoCloseable {
    public val config: RangingConfig
    public val localParams: SessionParams

    /**
     * Decode remote parameters and begin ranging.
     *
     * Each platform decodes [remoteParams] into its native representation
     * (Android: UwbAddress + ComplexChannel; iOS: NIDiscoveryToken).
     *
     * [backpressureStrategy] controls how measurements are buffered when the
     * consumer falls behind - kept separate from [config] because it is a
     * client-side delivery concern, not a UWB protocol parameter.
     *
     * This method consumes the prepared session - calling it again throws
     * [IllegalStateException].
     */
    public suspend fun startRanging(
        remoteParams: SessionParams,
        backpressureStrategy: BackpressureStrategy = BackpressureStrategy.KeepLatest,
    ): RangingSession

    override fun close()
}
