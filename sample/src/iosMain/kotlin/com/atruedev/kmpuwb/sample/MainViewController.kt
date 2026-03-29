package com.atruedev.kmpuwb.sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point. Use from SwiftUI:
 *
 * ```swift
 * import KmpUwbSample
 *
 * struct ContentView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController {
 *         MainViewControllerKt.MainViewController()
 *     }
 *     func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
 * }
 * ```
 */
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { RangingDemoScreen() }
