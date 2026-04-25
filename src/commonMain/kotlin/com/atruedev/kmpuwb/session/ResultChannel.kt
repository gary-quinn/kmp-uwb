package com.atruedev.kmpuwb.session

import com.atruedev.kmpuwb.config.BackpressureStrategy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

// 64 slots ≈ 12 s at typical 5 Hz update rate - absorbs UI jank without unbounded growth.
internal const val DEFAULT_BUFFER_CAPACITY = 64

internal fun createResultChannel(strategy: BackpressureStrategy): Channel<RangingResult> =
    when (strategy) {
        BackpressureStrategy.KeepLatest -> Channel(DEFAULT_BUFFER_CAPACITY, BufferOverflow.DROP_OLDEST)
        BackpressureStrategy.Unbounded -> Channel(Channel.UNLIMITED)
        BackpressureStrategy.KeepOldest -> Channel(DEFAULT_BUFFER_CAPACITY, BufferOverflow.DROP_LATEST)
    }
