package io.github.aedev.flow.data.settings

import android.content.Context

private var applicationContext: Context? = null

/** Points the settings store at the app's own preferences; called once from the host. */
fun initializeAppSettingsStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadAppSettingsJson(): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY, null)

actual fun saveAppSettingsJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY = "app_settings"
