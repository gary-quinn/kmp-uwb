package com.atruedev.kmpuwb.connector

import com.atruedev.kmpuwb.session.SessionParams

/**
 * Transport-agnostic interface for exchanging UWB session parameters with a remote peer.
 *
 * Applications implement this using their chosen out-of-band transport
 * (kmp-ble, NFC, WiFi Direct, etc.) to send local parameters and receive
 * the remote peer's parameters.
 *
 * ```kotlin
 * val session = adapter.startWithConnector(config) { localParams ->
 *     peripheral.write(uwbChar, localParams.toByteArray())
 *     val remoteBytes = peripheral.observe(uwbChar).first()
 *     SessionParams(remoteBytes)
 * }
 * ```
 */
public fun interface PeerConnector {
    /**
     * Exchange local session parameters with a remote peer.
     *
     * Implementations should:
     * 1. Send [localParams] to the remote peer over the OOB transport
     * 2. Receive the remote peer's session parameters
     * 3. Return the remote parameters as [SessionParams]
     *
     * @throws ConnectorException on transport failure, timeout, or invalid remote data
     */
    public suspend fun exchange(localParams: SessionParams): SessionParams
}
