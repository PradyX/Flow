package io.github.aedev.flow.data.navigation

import android.content.Context

private var applicationContext: Context? = null

/** Points navigation-state storage at the app's own preferences; called once from the host. */
fun initializeNavigationStateStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadNavigationStateJson(): String? =
    applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getString(KEY_NAVIGATION_STATE, null)

actual fun saveNavigationStateJson(json: String) {
    applicationContext
        ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(KEY_NAVIGATION_STATE, json)
        ?.apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY_NAVIGATION_STATE = "navigation_state"
