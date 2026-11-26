package io.github.aedev.flow.data.local

import android.content.Context

private var applicationContext: Context? = null

/** Points the imported-media record at the app's own preferences; called once from the host. */
fun initializeLocalMediaStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadLocalMediaJson(): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY, null)

actual fun saveLocalMediaJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY = "local_media"
