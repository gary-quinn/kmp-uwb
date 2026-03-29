@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.RangingConfig
import com.atruedev.kmpuwb.peer.Peer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSKeyedArchiver
import platform.NearbyInteraction.NISession

internal class IosPreparedSession(
    override val config: RangingConfig,
) : PreparedSession {
    private val niSession = NISession()
    private var consumed: Boolean = false

    override val localParams: SessionParams by lazy {
        val token =
            niSession.discoveryToken
                ?: error("NISession.discoveryToken is null — device may not support UWB")
        val data: NSData = NSKeyedArchiver.archivedDataWithRootObject(token)
        SessionParams(data.toByteArray())
    }

    override suspend fun startRanging(remotePeer: Peer): RangingSession {
        check(!consumed) { "PreparedSession has already been consumed" }
        consumed = true

        val session = IosRangingSession.fromPrepared(config, niSession)
        session.start(remotePeer)
        return session
    }

    override fun close() {
        if (!consumed) {
            niSession.invalidate()
        }
        consumed = true
    }

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        if (length == 0) return byteArrayOf()
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return bytes
    }
}
