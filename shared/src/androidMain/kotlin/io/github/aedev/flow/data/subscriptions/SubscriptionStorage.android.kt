package io.github.aedev.flow.data.subscriptions

import android.content.Context

private var applicationContext: Context? = null

/** Points subscription storage at the app's own preferences; called once from the host. */
fun initializeSubscriptionStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadSubscriptions(): SubscriptionBook? {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return null
    return SubscriptionBook.fromJson(prefs.getString(KEY_SUBSCRIPTIONS, null))
}

actual fun saveSubscriptions(book: SubscriptionBook) {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
    prefs.edit().putString(KEY_SUBSCRIPTIONS, SubscriptionBook.toJson(book)).apply()
}

private const val PREFS = "flow_onboarding"
private const val KEY_SUBSCRIPTIONS = "subscriptions"
