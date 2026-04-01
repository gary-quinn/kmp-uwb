package com.atruedev.kmpuwb.session

import androidx.core.uwb.RangingPosition
import com.atruedev.kmpuwb.peer.Peer
import com.atruedev.kmpuwb.peer.PeerAddress
import com.atruedev.kmpuwb.ranging.Angle
import com.atruedev.kmpuwb.ranging.Distance
import com.atruedev.kmpuwb.ranging.RangingMeasurement

internal fun androidx.core.uwb.RangingResult.toRangingResult(): RangingResult? =
    when (this) {
        is androidx.core.uwb.RangingResult.RangingResultPosition ->
            RangingResult.Position(
                peer = Peer(address = PeerAddress(this.device.address.address)),
                measurement = this.position.toMeasurement(),
            )
        is androidx.core.uwb.RangingResult.RangingResultPeerDisconnected ->
            RangingResult.PeerLost(
                peer = Peer(address = PeerAddress(this.device.address.address)),
            )
        else -> null
    }

internal fun RangingPosition.toMeasurement(): RangingMeasurement =
    RangingMeasurement(
        distance =
            this.distance
                ?.value
                ?.toDouble()
                ?.let { Distance.meters(it) },
        azimuth = this.azimuth?.let { Angle.degrees(it.value.toDouble()) },
        elevation = this.elevation?.let { Angle.degrees(it.value.toDouble()) },
    )
