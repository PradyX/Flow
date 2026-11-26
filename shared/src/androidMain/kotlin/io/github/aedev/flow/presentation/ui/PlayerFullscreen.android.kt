package io.github.aedev.flow.presentation.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun PlayerFullscreenEffect(fullscreen: Boolean) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    // The app is edge-to-edge already, so only the bars are hidden and shown again: turning decor
    // fitting off here and on again would fight the window setup that produced the current layout.
    DisposableEffect(activity, fullscreen) {
        if (fullscreen) activity?.enterFullscreen()
        onDispose { if (fullscreen) activity?.exitFullscreen() }
    }
}

@Composable
actual fun PlayerFullscreenBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}

/** Landscape, with the bars hidden until a swipe brings them back. */
private fun Activity.enterFullscreen() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

/** Back to the device's own orientation and the app's normal bars. */
private fun Activity.exitFullscreen() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
