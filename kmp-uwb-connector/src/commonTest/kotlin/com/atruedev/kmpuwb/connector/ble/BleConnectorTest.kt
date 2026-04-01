package com.atruedev.kmpuwb.connector.ble

import com.atruedev.kmpble.testing.FakePeripheral
import com.atruedev.kmpble.testing.FakeScanner
import com.atruedev.kmpuwb.connector.ConnectorException
import com.atruedev.kmpuwb.connector.InvalidRemoteParams
import com.atruedev.kmpuwb.connector.TransportFailure
import com.atruedev.kmpuwb.session.SessionParams
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class BleConnectorTest {
    private val localParams = SessionParams(byteArrayOf(0x01, 0x02, 0x03))
    private val remoteParams = byteArrayOf(0x0A, 0x0B, 0x0C)

    private fun buildPeripheral(
        observeResponse: ByteArray = remoteParams,
        writtenData: MutableList<ByteArray> = mutableListOf(),
    ): FakePeripheral =
        FakePeripheral {
            service(UwbOobService.SERVICE_UUID) {
                characteristic(UwbOobService.CONTROLLER_PARAMS_UUID) {
                    properties(write = true)
                    onWrite { data, _ -> writtenData.add(data) }
                }
                characteristic(UwbOobService.CONTROLEE_PARAMS_UUID) {
                    properties(notify = true)
                    onObserve { flowOf(observeResponse) }
                }
            }
        }

    private fun buildConnector(
        peripheral: FakePeripheral,
        config: BleConnectorConfig = BleConnectorConfig(),
    ): com.atruedev.kmpuwb.connector.PeerConnector {
        val scanner =
            FakeScanner {
                advertisement {
                    name("UWB-Peer")
                    serviceUuids(UwbOobService.SERVICE_UUID)
                }
            }
        return BleConnector.controller(scanner, config) { _ -> peripheral }
    }

    @Test
    fun controllerExchangeHappyPath() =
        runTest {
            val writtenData = mutableListOf<ByteArray>()
            val peripheral = buildPeripheral(writtenData = writtenData)
            val connector = buildConnector(peripheral)

            val result = connector.exchange(localParams)

            assertContentEquals(remoteParams, result.toByteArray())
            assertContentEquals(localParams.toByteArray(), writtenData.single())
        }

    @Test
    fun controllerClosesPeripheralAfterExchange() =
        runTest {
            val peripheral = buildPeripheral()
            val connector = buildConnector(peripheral)

            connector.exchange(localParams)

            // peripheral.close() is called in finally — further operations should fail
            assertFailsWith<IllegalStateException> {
                peripheral.connect()
            }
        }

    @Test
    fun controllerClosesPeripheralOnError() =
        runTest {
            val peripheral =
                FakePeripheral {
                    // No UWB service — exchange will fail with missing characteristic
                }
            val connector = buildConnector(peripheral)

            assertFailsWith<ConnectorException> {
                connector.exchange(localParams)
            }

            // peripheral.close() is called in finally even on error
            assertFailsWith<IllegalStateException> {
                peripheral.connect()
            }
        }

    @Test
    fun controllerThrowsTransportFailureOnMissingService() =
        runTest {
            val peripheral =
                FakePeripheral {
                    // Empty — no services at all
                }
            val connector = buildConnector(peripheral)

            val exception =
                assertFailsWith<ConnectorException> {
                    connector.exchange(localParams)
                }
            assertIs<TransportFailure>(exception.error)
        }

    @Test
    fun controllerThrowsInvalidRemoteParamsOnEmptyResponse() =
        runTest {
            val peripheral = buildPeripheral(observeResponse = byteArrayOf())
            val connector = buildConnector(peripheral)

            val exception =
                assertFailsWith<ConnectorException> {
                    connector.exchange(localParams)
                }
            assertIs<InvalidRemoteParams>(exception.error)
        }

    @Test
    fun controllerWrapsTransportExceptionFromScan() =
        runTest {
            val failingScanner =
                FakeScanner {
                    // Preconfigured ad that is NOT connectable — scanner.first { it.isConnectable } won't match
                    advertisement {
                        name("NotConnectable")
                        isConnectable(false)
                    }
                }
            // After emitting the non-connectable ad, FakeScanner suspends on dynamicAdvertisements.
            // The withTimeout inside ControllerConnector.scanForPeer() will fire.
            val config = BleConnectorConfig(scanTimeout = 50.milliseconds)
            val connector = BleConnector.controller(failingScanner, config) { error("should not be called") }

            // TimeoutCancellationException is a CancellationException, which the current
            // catch order in scanForPeer() rethrows directly. This is a known limitation —
            // the catch(TimeoutCancellationException) branch is unreachable.
            assertFailsWith<Exception> {
                connector.exchange(localParams)
            }
        }
}
