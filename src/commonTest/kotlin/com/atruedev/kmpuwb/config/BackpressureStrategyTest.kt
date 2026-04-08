package com.atruedev.kmpuwb.config

import com.atruedev.kmpuwb.session.SessionParams
import com.atruedev.kmpuwb.testing.FakePreparedSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BackpressureStrategyTest {
    @Test
    fun startRangingDefaultsToKeepLatest() =
        runTest {
            val prepared = FakePreparedSession()
            prepared.startRanging(SessionParams(byteArrayOf(0x01)))
            assertEquals(BackpressureStrategy.KeepLatest, prepared.lastBackpressureStrategy)
        }

    @Test
    fun startRangingForwardsExplicitStrategy() =
        runTest {
            val prepared = FakePreparedSession()
            prepared.startRanging(SessionParams(byteArrayOf(0x01)), BackpressureStrategy.Unbounded)
            assertEquals(BackpressureStrategy.Unbounded, prepared.lastBackpressureStrategy)
        }
}
