package com.atruedev.kmpuwb.connector.ble

import com.atruedev.kmpble.BleData
import com.atruedev.kmpble.error.GattStatus
import com.atruedev.kmpble.gatt.WriteType
import com.atruedev.kmpble.peripheral.Peripheral
import com.atruedev.kmpble.peripheral.toPeripheral
import com.atruedev.kmpble.scanner.Scanner
import com.atruedev.kmpble.server.AdvertiseConfig
import com.atruedev.kmpble.server.Advertiser
import com.atruedev.kmpble.server.GattServer
import com.atruedev.kmpuwb.connector.ConnectorException
import com.atruedev.kmpuwb.connector.ExchangeTimedOut
import com.atruedev.kmpuwb.connector.InvalidRemoteParams
import com.atruedev.kmpuwb.connector.PeerConnector
import com.atruedev.kmpuwb.connector.TransportFailure
import com.atruedev.kmpuwb.session.SessionParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.uuid.ExperimentalUuidApi

/**
 * [PeerConnector] implementation using BLE for automatic OOB parameter exchange.
 *
 * ```kotlin
 * // Controller — scans for a controlee, connects, exchanges params
 * val scanner = Scanner { filters { match { serviceUuid(UwbOobService.SERVICE_UUID) } } }
 * val connector = BleConnector.controller(scanner)
 * val session = adapter.startWithConnector(config, connector)
 *
 * // Controlee — advertises, waits for controller to connect and exchange
 * val connector = BleConnector.controlee()
 * val session = adapter.startWithConnector(config, connector)
 * ```
 */
public object BleConnector {
    public fun controller(
        scanner: Scanner,
        config: BleConnectorConfig = BleConnectorConfig(),
    ): PeerConnector = ControllerConnector(scanner, config)

    public fun controlee(config: BleConnectorConfig = BleConnectorConfig()): PeerConnector = ControleeConnector(config)
}

@OptIn(ExperimentalUuidApi::class)
private class ControllerConnector(
    private val scanner: Scanner,
    private val config: BleConnectorConfig,
) : PeerConnector {
    override suspend fun exchange(localParams: SessionParams): SessionParams {
        val peripheral = scanForPeer()
        try {
            return exchangeWithPeer(peripheral, localParams)
        } finally {
            safeDisconnect(peripheral)
        }
    }

    private suspend fun scanForPeer(): Peripheral {
        try {
            val advertisement =
                withTimeout(config.scanTimeout) {
                    scanner.advertisements
                        .first { it.isConnectable }
                }
            return advertisement.toPeripheral()
        } catch (e: CancellationException) {
            throw e
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw ConnectorException(
                ExchangeTimedOut("No UWB peer found within ${config.scanTimeout}"),
            )
        } catch (e: Exception) {
            throw ConnectorException(
                TransportFailure("BLE scan failed: ${e.message}", cause = e),
            )
        }
    }

    private suspend fun exchangeWithPeer(
        peripheral: Peripheral,
        localParams: SessionParams,
    ): SessionParams {
        try {
            peripheral.connect(config.connectionOptions)

            val controllerChar =
                peripheral.findCharacteristic(
                    UwbOobService.SERVICE_UUID,
                    UwbOobService.CONTROLLER_PARAMS_UUID,
                ) ?: throw ConnectorException(
                    TransportFailure("Controller params characteristic not found"),
                )

            val controleeChar =
                peripheral.findCharacteristic(
                    UwbOobService.SERVICE_UUID,
                    UwbOobService.CONTROLEE_PARAMS_UUID,
                ) ?: throw ConnectorException(
                    TransportFailure("Controlee params characteristic not found"),
                )

            val remoteBytes =
                withTimeout(config.exchangeTimeout) {
                    val observation = peripheral.observeValues(controleeChar)

                    peripheral.write(
                        controllerChar,
                        localParams.toByteArray(),
                        WriteType.WithResponse,
                    )

                    observation.first()
                }

            if (remoteBytes.isEmpty()) {
                throw ConnectorException(
                    InvalidRemoteParams("Received empty params from controlee"),
                )
            }

            return SessionParams(remoteBytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ConnectorException) {
            throw e
        } catch (e: Exception) {
            throw ConnectorException(
                TransportFailure("BLE exchange failed: ${e.message}", cause = e),
            )
        }
    }

    private fun safeDisconnect(peripheral: Peripheral) {
        try {
            peripheral.close()
        } catch (_: Exception) {
            // cleanup errors must not mask the original
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private class ControleeConnector(
    private val config: BleConnectorConfig,
) : PeerConnector {
    override suspend fun exchange(localParams: SessionParams): SessionParams {
        val remoteParamsDeferred = CompletableDeferred<ByteArray>()
        val connectedDevice = CompletableDeferred<com.atruedev.kmpble.Identifier>()
        val localBytes = localParams.toByteArray()

        val server =
            GattServer {
                service(UwbOobService.SERVICE_UUID) {
                    characteristic(UwbOobService.CONTROLLER_PARAMS_UUID) {
                        properties {
                            write = true
                        }
                        permissions {
                            write = true
                        }
                        onWrite { device, data, _ ->
                            connectedDevice.complete(device)
                            remoteParamsDeferred.complete(data.toByteArray())
                            GattStatus.Success
                        }
                    }
                    characteristic(UwbOobService.CONTROLEE_PARAMS_UUID) {
                        properties {
                            read = true
                            notify = true
                        }
                        permissions {
                            read = true
                        }
                        onRead { _ ->
                            BleData(localBytes)
                        }
                    }
                }
            }

        val advertiser = Advertiser()

        try {
            server.open()

            advertiser.startAdvertising(
                AdvertiseConfig(
                    serviceUuids = listOf(UwbOobService.SERVICE_UUID),
                    connectable = true,
                ),
            )

            val remoteBytes =
                withTimeout(config.scanTimeout + config.exchangeTimeout) {
                    val bytes = remoteParamsDeferred.await()
                    val device = connectedDevice.await()

                    server.notify(
                        UwbOobService.CONTROLEE_PARAMS_UUID,
                        device,
                        BleData(localBytes),
                    )

                    bytes
                }

            if (remoteBytes.isEmpty()) {
                throw ConnectorException(
                    InvalidRemoteParams("Received empty params from controller"),
                )
            }

            return SessionParams(remoteBytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ConnectorException) {
            throw e
        } catch (e: Exception) {
            throw ConnectorException(
                TransportFailure("BLE controlee exchange failed: ${e.message}", cause = e),
            )
        } finally {
            try {
                advertiser.stopAdvertising()
            } catch (_: Exception) {
                // cleanup
            }
            try {
                advertiser.close()
            } catch (_: Exception) {
                // cleanup
            }
            try {
                server.close()
            } catch (_: Exception) {
                // cleanup
            }
        }
    }
}
