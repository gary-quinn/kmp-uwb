package com.atruedev.kmpuwb.connector

import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.session.RangingSession

/**
 * Prepare a UWB session, exchange parameters via [connector], and start ranging.
 *
 * This orchestrates the full two-phase flow:
 * 1. Prepare a session and obtain local parameters
 * 2. Exchange parameters with the remote peer via [connector]
 * 3. Decode the remote parameters and start ranging
 *
 * If any step fails, platform resources are released automatically.
 *
 * ```kotlin
 * val session = adapter.startWithConnector(config) { localParams ->
 *     peripheral.write(uwbChar, localParams.toByteArray())
 *     val remoteBytes = peripheral.observe(uwbChar).first()
 *     SessionParams(remoteBytes)
 * }
 * session.rangingResults.collect { result -> ... }
 * ```
 *
 * @throws ConnectorException if the OOB exchange fails
 */
public suspend fun UwbAdapter.startWithConnector(
    config: RangingConfig,
    connector: PeerConnector,
): RangingSession {
    val prepared = prepareSession(config)
    try {
        val remoteParams = connector.exchange(prepared.localParams)
        val remotePeer = Peer(address = PeerAddress(remoteParams.toByteArray()))
        return prepared.startRanging(remotePeer)
    } catch (e: Throwable) {
        prepared.close()
        throw e
    }
}
