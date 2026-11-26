package io.github.aedev.flow.data.downloads

import android.content.Context

private var applicationContext: Context? = null

/** Points the download record at the app's own preferences; called once from the host. */
fun initializeDownloadsStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadDownloadsJson(): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY, null)

actual fun saveDownloadsJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY = "downloads"
