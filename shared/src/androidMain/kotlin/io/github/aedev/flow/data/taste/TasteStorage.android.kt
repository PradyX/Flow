package io.github.aedev.flow.data.taste

import android.content.Context

private var applicationContext: Context? = null

/** Points taste storage at the app's own preferences; called once from the host. */
fun initializeTasteStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadUserTaste(): UserTaste? {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return null
    return UserTaste.fromJson(prefs.getString(KEY_TASTE, null))
}

actual fun saveUserTaste(taste: UserTaste) {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
    prefs.edit().putString(KEY_TASTE, UserTaste.toJson(taste)).apply()
}

actual fun loadHomeFeedback(): HomeFeedback? {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return null
    return HomeFeedback.fromJson(prefs.getString(KEY_FEEDBACK, null))
}

actual fun saveHomeFeedback(feedback: HomeFeedback) {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
    prefs.edit().putString(KEY_FEEDBACK, HomeFeedback.toJson(feedback)).apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY_TASTE = "user_taste"
private const val KEY_FEEDBACK = "home_feedback"
