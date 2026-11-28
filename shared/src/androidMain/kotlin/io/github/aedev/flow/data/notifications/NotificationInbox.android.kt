package io.github.aedev.flow.data.notifications

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private var applicationContext: Context? = null

/** Points the inbox at the app's own preferences; called once from the host. */
fun initializeNotificationInboxStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun loadNotificationInbox(): NotificationInbox? {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return null
    return NotificationInbox.decode(prefs.getString(KEY, null))
}

actual fun saveNotificationInbox(inbox: NotificationInbox) {
    val prefs = applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
    prefs.edit().putString(KEY, inbox.encode()).apply()
}

actual fun deviceDayNumber(epochMillis: Long): Long =
    Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toEpochDay()

actual fun deviceTimeOfDayLabel(epochMillis: Long): String =
    Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

actual fun deviceWeekdayLabel(epochMillis: Long): String =
    Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .dayOfWeek
        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())

actual fun deviceDateLabel(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

private const val PREFS = "flow_onboarding"
private const val KEY = "notifications"
