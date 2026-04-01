package com.atruedev.kmpuwb.session

import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wire format for Android UWB session parameters exchanged over OOB transports.
 *
 * Format (big-endian):
 * ```
 * [version: 1B] [addressLen: 1B] [address: NB] [channel: 1B] [preambleIndex: 1B] [sessionId: 4B]
 * ```
 */
internal object SessionParamsCodec {
    private const val VERSION_ANDROID: Byte = 0x01
    private const val HEADER_SIZE = 2 // version + addressLen
    private const val CHANNEL_AND_PREAMBLE_SIZE = 2
    private const val SESSION_ID_SIZE = 4

    data class DecodedParams(
        val address: ByteArray,
        val channel: Int,
        val preambleIndex: Int,
        val sessionId: Int,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DecodedParams) return false
            return address.contentEquals(other.address) &&
                channel == other.channel &&
                preambleIndex == other.preambleIndex &&
                sessionId == other.sessionId
        }

        override fun hashCode(): Int {
            var result = address.contentHashCode()
            result = 31 * result + channel
            result = 31 * result + preambleIndex
            result = 31 * result + sessionId
            return result
        }
    }

    fun encode(
        address: UwbAddress,
        complexChannel: UwbComplexChannel,
        sessionId: Int,
    ): SessionParams {
        val addressBytes = address.address
        val totalSize = HEADER_SIZE + addressBytes.size + CHANNEL_AND_PREAMBLE_SIZE + SESSION_ID_SIZE
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.put(VERSION_ANDROID)
        buffer.put(addressBytes.size.toByte())
        buffer.put(addressBytes)
        buffer.put(complexChannel.channel.toByte())
        buffer.put(complexChannel.preambleIndex.toByte())
        buffer.putInt(sessionId)

        return SessionParams(buffer.array())
    }

    fun decode(params: SessionParams): DecodedParams {
        val bytes = params.toByteArray()
        require(bytes.size >= HEADER_SIZE) { "SessionParams too short: ${bytes.size} bytes" }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val version = buffer.get()
        require(version == VERSION_ANDROID) {
            "Incompatible SessionParams version: 0x${version.toUByte().toString(16)} (expected Android 0x01)"
        }

        val addressLen = buffer.get().toInt() and 0xFF
        require(buffer.remaining() >= addressLen + CHANNEL_AND_PREAMBLE_SIZE + SESSION_ID_SIZE) {
            "SessionParams truncated: expected ${addressLen + CHANNEL_AND_PREAMBLE_SIZE + SESSION_ID_SIZE} more bytes, got ${buffer.remaining()}"
        }

        val address = ByteArray(addressLen)
        buffer.get(address)
        val channel = buffer.get().toInt() and 0xFF
        val preambleIndex = buffer.get().toInt() and 0xFF
        val sessionId = buffer.getInt()

        return DecodedParams(
            address = address,
            channel = channel,
            preambleIndex = preambleIndex,
            sessionId = sessionId,
        )
    }
}
