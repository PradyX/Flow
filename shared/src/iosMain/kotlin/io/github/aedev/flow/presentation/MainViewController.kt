package io.github.aedev.flow.presentation

import androidx.compose.ui.window.ComposeUIViewController

/** iOS entry point — hosted by the `iosApp` Swift shell. */
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
