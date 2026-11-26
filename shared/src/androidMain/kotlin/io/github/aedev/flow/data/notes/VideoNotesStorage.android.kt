package io.github.aedev.flow.data.notes

import android.content.Context

private var applicationContext: Context? = null

/** Points the notes store at the app's own preferences; called once from the host. */
fun initializeVideoNotesStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadVideoNotesJson(): String? =
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.getString(KEY, null)

actual fun saveVideoNotesJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY = "video_notes"
