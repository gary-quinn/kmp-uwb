package com.atruedev.kmpuwb.connector

/**
 * Errors that occur during out-of-band peer parameter exchange.
 */
public sealed interface ConnectorError {
    public val message: String
    public val cause: Throwable?
}

/** The OOB transport failed to complete the exchange within the expected time. */
public data class ExchangeTimedOut(
    override val message: String,
    override val cause: Throwable? = null,
) : ConnectorError

/** The remote peer sent parameters that could not be decoded. */
public data class InvalidRemoteParams(
    override val message: String,
    override val cause: Throwable? = null,
) : ConnectorError

/** The underlying transport (BLE, NFC, WiFi) encountered a failure. */
public data class TransportFailure(
    override val message: String,
    override val cause: Throwable? = null,
) : ConnectorError

/**
 * Throwable wrapper for [ConnectorError], suitable for throwing from
 * [PeerConnector.exchange] implementations.
 */
public class ConnectorException(
    public val error: ConnectorError,
) : Exception(error.message, error.cause)
