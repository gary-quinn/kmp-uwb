package com.atruedev.kmpuwb.adapter

import android.content.Context

public object KmpUwb {
    internal lateinit var appContext: Context
        private set

    public fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun requireContext(): Context {
        check(::appContext.isInitialized) {
            "Call KmpUwb.init(context) in your Application.onCreate() before using kmp-uwb"
        }
        return appContext
    }
}
