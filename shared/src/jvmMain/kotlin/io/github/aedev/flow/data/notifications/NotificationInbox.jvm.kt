package io.github.aedev.flow.data.notifications

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

actual fun loadNotificationInbox(): NotificationInbox? =
    runCatching { NotificationInbox.decode(inboxFile().takeIf { it.isFile }?.readText()) }.getOrNull()

actual fun saveNotificationInbox(inbox: NotificationInbox) {
    runCatching {
        val file = inboxFile()
        file.parentFile?.mkdirs()
        file.writeText(inbox.encode())
    }
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
        .getDisplayName(TextStyle.FULL, Locale.getDefault())

actual fun deviceDateLabel(epochMillis: Long): String =
    Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private fun inboxFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-notifications.json")
