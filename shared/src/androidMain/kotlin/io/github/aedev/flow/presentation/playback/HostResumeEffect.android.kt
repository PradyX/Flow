package io.github.aedev.flow.presentation.playback

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun HostResumeEffect(onResume: () -> Unit) {
    val activity = LocalContext.current.findActivity() ?: return
    val current by rememberUpdatedState(onResume)
    DisposableEffect(activity) {
        val callbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(resumed: Activity) {
                    if (resumed === activity) current()
                }

                override fun onActivityCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?,
                ) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    activity: Activity,
                    outState: Bundle,
                ) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        activity.application.registerActivityLifecycleCallbacks(callbacks)
        onDispose { activity.application.unregisterActivityLifecycleCallbacks(callbacks) }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
