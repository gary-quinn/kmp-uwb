package com.atruedev.kmpuwb.session

import androidx.core.uwb.RangingPosition
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbDevice
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import androidx.core.uwb.RangingMeasurement as AndroidRangingMeasurement
import androidx.core.uwb.RangingResult as AndroidRangingResult

@RunWith(RobolectricTestRunner::class)
class AndroidRangingResultMapperTest {
    private val testAddress = byteArrayOf(0x01, 0x02)
    private val testDevice = UwbDevice(UwbAddress(testAddress))

    @Test
    fun positionResultMapsDistanceAzimuthElevation() {
        val position =
            RangingPosition(
                distance = AndroidRangingMeasurement(2.5f),
                azimuth = AndroidRangingMeasurement(15.0f),
                elevation = AndroidRangingMeasurement(-10.0f),
                elapsedRealtimeNanos = 0L,
            )
        val platformResult = AndroidRangingResult.RangingResultPosition(testDevice, position)

        val result = platformResult.toRangingResult()

        assertNotNull(result)
        assertIs<RangingResult.Position>(result)
        assertEquals(2.5, result.measurement.distance?.meters)
        assertEquals(15.0, result.measurement.azimuth?.degrees)
        assertEquals(-10.0, result.measurement.elevation?.degrees)
    }

    @Test
    fun positionResultWithNullDistanceMapsToNull() {
        val position =
            RangingPosition(
                distance = null,
                azimuth = AndroidRangingMeasurement(5.0f),
                elevation = null,
                elapsedRealtimeNanos = 0L,
            )
        val platformResult = AndroidRangingResult.RangingResultPosition(testDevice, position)

        val result = platformResult.toRangingResult()

        assertNotNull(result)
        assertIs<RangingResult.Position>(result)
        assertNull(result.measurement.distance)
        assertNotNull(result.measurement.azimuth)
        assertNull(result.measurement.elevation)
    }

    @Test
    fun positionResultWithAllNullMeasurements() {
        val position =
            RangingPosition(
                distance = null,
                azimuth = null,
                elevation = null,
                elapsedRealtimeNanos = 0L,
            )
        val platformResult = AndroidRangingResult.RangingResultPosition(testDevice, position)

        val result = platformResult.toRangingResult()

        assertNotNull(result)
        assertIs<RangingResult.Position>(result)
        assertNull(result.measurement.distance)
        assertNull(result.measurement.azimuth)
        assertNull(result.measurement.elevation)
    }

    @Test
    fun peerDisconnectedMapsToPeerLost() {
        val platformResult = AndroidRangingResult.RangingResultPeerDisconnected(testDevice, 0)

        val result = platformResult.toRangingResult()

        assertNotNull(result)
        assertIs<RangingResult.PeerLost>(result)
    }

    @Test
    fun peerAddressPreservedInPosition() {
        val position =
            RangingPosition(
                distance = AndroidRangingMeasurement(1.0f),
                azimuth = null,
                elevation = null,
                elapsedRealtimeNanos = 0L,
            )
        val platformResult = AndroidRangingResult.RangingResultPosition(testDevice, position)

        val result = platformResult.toRangingResult()

        assertIs<RangingResult.Position>(result)
        assertEquals(
            testAddress.toList(),
            result.peer.address
                .toByteArray()
                .toList(),
        )
    }

    @Test
    fun peerAddressPreservedInPeerLost() {
        val platformResult = AndroidRangingResult.RangingResultPeerDisconnected(testDevice, 0)

        val result = platformResult.toRangingResult()

        assertIs<RangingResult.PeerLost>(result)
        assertEquals(
            testAddress.toList(),
            result.peer.address
                .toByteArray()
                .toList(),
        )
    }

    @Test
    fun initializedResultReturnsNull() {
        val platformResult = AndroidRangingResult.RangingResultInitialized(testDevice)

        val result = platformResult.toRangingResult()

        assertNull(result)
    }

    @Test
    fun failureResultReturnsNull() {
        val platformResult = AndroidRangingResult.RangingResultFailure(testDevice, 0)

        val result = platformResult.toRangingResult()

        assertNull(result)
    }
}
