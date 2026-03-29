package com.atruedev.kmpuwb.sample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

private lateinit var clipboardContext: Context

fun initClipboard(context: Context) {
    clipboardContext = context.applicationContext
}

actual fun copyToClipboard(text: String) {
    val clipboard =
        clipboardContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("UWB Params", text))
}
