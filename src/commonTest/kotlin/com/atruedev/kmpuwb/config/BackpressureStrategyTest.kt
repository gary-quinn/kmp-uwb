package com.atruedev.kmpuwb.config

import kotlin.test.Test
import kotlin.test.assertEquals

class BackpressureStrategyTest {
    @Test
    fun latestIsDefaultInRangingConfig() {
        val config = rangingConfig { role = RangingRole.CONTROLEE }
        assertEquals(BackpressureStrategy.Latest, config.backpressureStrategy)
    }

    @Test
    fun dslBuilderAcceptsStrategy() {
        val config =
            rangingConfig {
                role = RangingRole.CONTROLLER
                backpressureStrategy = BackpressureStrategy.Buffer
            }
        assertEquals(BackpressureStrategy.Buffer, config.backpressureStrategy)
    }
}
