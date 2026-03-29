package com.atruedev.kmpuwb.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.atruedev.kmpuwb.session.SessionParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Minimal Android entry point for the UWB ranging demo.
 *
 * Replace the [stubConnector] with a real [PeerConnector] implementation
 * that exchanges SessionParams over BLE (via kmp-ble), NFC, or WiFi.
 */
class SampleActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var demo: RangingDemo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logView =
            TextView(this).apply {
                setPadding(32, 32, 32, 32)
                textSize = 14f
            }
        setContentView(logView)

        demo =
            RangingDemo(
                scope = scope,
                connector = stubConnector(),
                log = { message ->
                    Log.d("kmp-uwb-sample", message)
                    runOnUiThread {
                        logView.append("$message\n")
                    }
                },
            )
        demo?.start()
    }

    override fun onDestroy() {
        demo?.stop()
        scope.cancel()
        super.onDestroy()
    }
}

/**
 * Stub connector for demonstration. In a real app, replace with a
 * PeerConnector that exchanges params over BLE:
 *
 * ```kotlin
 * PeerConnector { localParams ->
 *     peripheral.write(uwbChar, localParams.toByteArray())
 *     SessionParams(peripheral.observe(uwbChar).first())
 * }
 * ```
 */
private fun stubConnector() =
    com.atruedev.kmpuwb.connector.PeerConnector { localParams ->
        // In a real app, send localParams over BLE and receive remote params
        // For demo purposes, echo back the local params
        SessionParams(localParams.toByteArray())
    }
