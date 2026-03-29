package com.atruedev.kmpuwb.session

import android.content.Context
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbDevice
import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.peer.Peer
import java.nio.ByteBuffer

internal class AndroidPreparedSession(
    override val config: RangingConfig,
    private val context: Context,
    private val sessionScope: UwbClientSessionScope,
) : PreparedSession {
    private var consumed: Boolean = false

    override val localParams: SessionParams by lazy {
        encodeLocalParams()
    }

    override suspend fun startRanging(remotePeer: Peer): RangingSession {
        check(!consumed) { "PreparedSession has already been consumed" }
        consumed = true

        val peerDevices = listOf(UwbDevice(UwbAddress(remotePeer.address.toByteArray())))
        val complexChannel =
            if (sessionScope is UwbControllerSessionScope) {
                sessionScope.uwbComplexChannel
            } else {
                UwbComplexChannel(config.channel, 0)
            }

        val rangingParameters =
            RangingParameters(
                uwbConfigType = RangingParameters.CONFIG_UNICAST_DS_TWR,
                sessionId = config.sessionId,
                sessionKeyInfo = config.sessionKey ?: byteArrayOf(),
                complexChannel = complexChannel,
                peerDevices = peerDevices,
                updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
                subSessionId = 0,
                subSessionKeyInfo = null,
            )

        return AndroidRangingSession.fromPrepared(config, context, sessionScope, rangingParameters)
    }

    override fun close() {
        consumed = true
    }

    private fun encodeLocalParams(): SessionParams {
        val localAddress = sessionScope.localAddress.address
        val complexChannel =
            if (sessionScope is UwbControllerSessionScope) {
                sessionScope.uwbComplexChannel
            } else {
                UwbComplexChannel(config.channel, 0)
            }

        val sessionKey = config.sessionKey ?: byteArrayOf()
        val buffer =
            ByteBuffer.allocate(
                2 + 1 + 1 + 4 + sessionKey.size,
            )
        buffer.put(localAddress)
        buffer.put(complexChannel.channel.toByte())
        buffer.put(complexChannel.preambleIndex.toByte())
        buffer.putInt(config.sessionId)
        buffer.put(sessionKey)

        return SessionParams(buffer.array())
    }
}
