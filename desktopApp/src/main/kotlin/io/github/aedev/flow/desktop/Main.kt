package io.github.aedev.flow.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.aedev.flow.presentation.App
import io.github.aedev.flow.presentation.ui.LocalShareAction

/** Desktop entry point — the same shared [App] as Android and iOS, hosted in a native window. */
fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Flow",
            state = rememberWindowState(size = DpSize(1100.dp, 800.dp)),
        ) {
            // No share sheet on the desktop; the link opens in the user's browser instead.
            val uriHandler = LocalUriHandler.current
            CompositionLocalProvider(LocalShareAction provides { _, url -> uriHandler.openUri(url) }) {
                App()
            }
        }
    }
