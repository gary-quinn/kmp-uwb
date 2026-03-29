@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.atruedev.kmpuwb.connector.ble

import com.atruedev.kmpble.connection.ConnectionOptions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public data class BleConnectorConfig(
    val scanTimeout: Duration = 10.seconds,
    val exchangeTimeout: Duration = 5.seconds,
    val connectionOptions: ConnectionOptions = ConnectionOptions(timeout = 10.seconds),
)
