package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.BackpressureStrategy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

internal const val DEFAULT_BUFFER_CAPACITY = 64

internal fun createResultChannel(strategy: BackpressureStrategy): Channel<RangingResult> =
    when (strategy) {
        BackpressureStrategy.Latest -> Channel(DEFAULT_BUFFER_CAPACITY, BufferOverflow.DROP_OLDEST)
        BackpressureStrategy.Buffer -> Channel(Channel.UNLIMITED)
        BackpressureStrategy.Drop -> Channel(DEFAULT_BUFFER_CAPACITY, BufferOverflow.DROP_LATEST)
    }
