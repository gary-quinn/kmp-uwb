package com.atruedev.kmpuwb.adapter

import android.content.Context
import androidx.startup.Initializer

/**
 * Auto-initializes [KmpUwb] via AndroidX App Startup.
 *
 * This runs automatically before the app's `Application.onCreate()`,
 * so consumers never need to call `KmpUwb.init(context)` manually.
 *
 * To disable auto-init and call `KmpUwb.init()` yourself, add to your
 * app's AndroidManifest.xml:
 * ```xml
 * <provider
 *     android:name="androidx.startup.InitializationProvider"
 *     android:authorities="${applicationId}.androidx-startup"
 *     tools:node="merge">
 *     <meta-data
 *         android:name="com.atruedev.kmpuwb.adapter.KmpUwbInitializer"
 *         tools:node="remove" />
 * </provider>
 * ```
 */
public class KmpUwbInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        KmpUwb.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
