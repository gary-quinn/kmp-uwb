package com.atruedev.kmpuwb.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class RangingConfigTest {
    @Test
    fun dslBuilderCreatesConfig() {
        val config =
            rangingConfig {
                role = RangingRole.CONTROLLER
                angleOfArrival = false
                channel = 5
            }

        assertEquals(RangingRole.CONTROLLER, config.role)
        assertEquals(false, config.angleOfArrival)
        assertEquals(5, config.channel)
    }

    @Test
    fun defaultValuesAreApplied() {
        val config = rangingConfig { role = RangingRole.CONTROLEE }

        assertEquals(RangingRole.CONTROLEE, config.role)
        assertEquals(200.milliseconds, config.rangingInterval)
        assertTrue(config.angleOfArrival)
        assertEquals(StsMode.DYNAMIC, config.stsMode)
        assertEquals(9, config.channel)
        assertEquals(0, config.sessionId)
    }

    @Test
    fun invalidChannelThrows() {
        assertFailsWith<IllegalArgumentException> {
            rangingConfig {
                role = RangingRole.CONTROLLER
                channel = 7
            }
        }
    }

    @Test
    fun rangingIntervalBelowMinimumThrows() {
        assertFailsWith<IllegalArgumentException> {
            rangingConfig {
                role = RangingRole.CONTROLLER
                rangingInterval = 10.milliseconds
            }
        }
    }

    @Test
    fun channel5IsValid() {
        val config =
            rangingConfig {
                role = RangingRole.CONTROLLER
                channel = 5
            }
        assertEquals(5, config.channel)
    }

    @Test
    fun channel9IsValid() {
        val config =
            rangingConfig {
                role = RangingRole.CONTROLLER
                channel = 9
            }
        assertEquals(9, config.channel)
    }
}
