package io.github.aedev.flow.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What a picker launch needs from the composition; the remembered object reads it when called. */
private class PickerLauncher {
    var launch: (List<String>, Boolean, (List<String>) -> Unit) -> Unit = { _, _, _ -> }
}

@Composable
internal actual fun rememberOnboardingPlatform(): OnboardingPlatform {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notifications = remember { mutableStateOf(notificationsGranted(context)) }
    val background = remember { mutableStateOf(backgroundUnrestricted(context)) }
    val pending = remember { mutableStateOf<((List<String>) -> Unit)?>(null) }
    val picker = remember { PickerLauncher() }

    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notifications.value = granted
        }

    fun read(
        uri: Uri,
        callback: (List<String>) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            val text =
                runCatching {
                    context.contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                }.getOrNull()
            if (text != null) withContext(Dispatchers.Main) { callback(listOf(text)) }
        }
    }

    val single =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val callback = pending.value ?: return@rememberLauncherForActivityResult
            pending.value = null
            if (uri != null) read(uri, callback)
        }
    val several =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val callback = pending.value ?: return@rememberLauncherForActivityResult
            pending.value = null
            uris.forEach { uri -> read(uri, callback) }
        }

    picker.launch = { mimeTypes, many, callback ->
        pending.value = callback
        val types = mimeTypes.toTypedArray()
        if (many) several.launch(types) else single.launch(types)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifications.value = notificationsGranted(context)
        background.value = backgroundUnrestricted(context)
    }
    LaunchedEffect(Unit) { notifications.value = notificationsGranted(context) }

    return remember(context) {
        object : OnboardingPlatform {
            override val notificationsAllowed: Boolean get() = notifications.value

            override val backgroundUnrestricted: Boolean get() = background.value

            override fun requestNotifications() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            override fun openNotificationSettings() {
                val intent =
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }

            override fun requestUnrestrictedBackground() {
                val intent =
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }

            override fun pickFiles(
                mimeTypes: List<String>,
                several: Boolean,
                onPicked: (List<String>) -> Unit,
            ) = picker.launch(mimeTypes, several, onPicked)
        }
    }
}

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun backgroundUnrestricted(context: Context): Boolean {
    val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return power.isIgnoringBatteryOptimizations(context.packageName)
}
