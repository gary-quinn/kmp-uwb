package com.atruedev.kmpuwb.connector

import com.atruedev.kmpuwb.adapter.UwbAdapter
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.session.RangingSession

/**
 * Prepare a UWB session, exchange parameters via [connector], and start ranging.
 *
 * Orchestrates: prepare → exchange → decode → start. If any step fails,
 * platform resources are released automatically.
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
        return prepared.startRanging(remoteParams)
    } catch (e: Throwable) {
        prepared.close()
        throw e
    }
}
