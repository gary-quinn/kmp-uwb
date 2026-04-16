package com.atruedev.kmpuwb.adapter

import android.content.Context

public object KmpUwb {
    private var contextHolder: Context? = null

    internal val appContext: Context
        get() =
            contextHolder ?: error(
                "Call KmpUwb.init(context) in your Application.onCreate() before using kmp-uwb",
            )

    public fun init(context: Context) {
        contextHolder = context.applicationContext
    }

    internal fun requireContext(): Context = appContext

    internal fun reset() {
        contextHolder = null
    }
}
