package io.github.aedev.flow.data.notifications

/**
 * The inbox's sections, in the order they are shown: what was unread when the page opened, then the
 * rest by calendar day, as the app groups its own rows.
 */
enum class InboxSection {
    NEW,
    TODAY,
    YESTERDAY,
    LAST_WEEK,
    EARLIER,
}

/**
 * The section one row falls in. [newIds] is the unread snapshot the page took when it opened, so a
 * row keeps its New marker for the rest of the visit even after the inbox is marked read.
 */
fun sectionOf(
    notification: InboxNotification,
    newIds: Set<String>,
    todayNumber: Long,
): InboxSection {
    if (notification.videoId in newIds) return InboxSection.NEW
    val day = deviceDayNumber(notification.arrivedAtMillis)
    return when {
        day >= todayNumber -> InboxSection.TODAY
        day == todayNumber - 1 -> InboxSection.YESTERDAY
        day >= todayNumber - LAST_WEEK_DAYS -> InboxSection.LAST_WEEK
        else -> InboxSection.EARLIER
    }
}

/** The row's time line: how long ago within today, then a clock, a weekday, and finally a date. */
sealed interface InboxTime {
    data object JustNow : InboxTime

    data class Minutes(
        val value: Long,
    ) : InboxTime

    data class Hours(
        val value: Long,
    ) : InboxTime

    data class Days(
        val value: Long,
    ) : InboxTime

    /** The moment as a clock, for a row from yesterday. */
    data class Clock(
        val text: String,
    ) : InboxTime

    data class Weekday(
        val text: String,
    ) : InboxTime

    data class Date(
        val text: String,
    ) : InboxTime
}

fun inboxTime(
    notification: InboxNotification,
    section: InboxSection,
    nowMillis: Long,
): InboxTime =
    when (section) {
        InboxSection.NEW, InboxSection.TODAY -> relativeTime(notification.arrivedAtMillis, nowMillis)
        InboxSection.YESTERDAY -> InboxTime.Clock(deviceTimeOfDayLabel(notification.arrivedAtMillis))
        InboxSection.LAST_WEEK -> InboxTime.Weekday(deviceWeekdayLabel(notification.arrivedAtMillis))
        InboxSection.EARLIER -> InboxTime.Date(deviceDateLabel(notification.arrivedAtMillis))
    }

private fun relativeTime(
    thenMillis: Long,
    nowMillis: Long,
): InboxTime {
    val elapsedMs = (nowMillis - thenMillis).coerceAtLeast(0)
    val minutes = elapsedMs / MINUTE_MS
    val hours = elapsedMs / HOUR_MS
    return when {
        minutes < 1 -> InboxTime.JustNow
        hours < 1 -> InboxTime.Minutes(minutes)
        hours < 24 -> InboxTime.Hours(hours)
        else -> InboxTime.Days(hours / 24)
    }
}

private const val LAST_WEEK_DAYS = 7L
private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60L * MINUTE_MS

/**
 * The local calendar day [epochMillis] falls on, counted in days. Only differences are used, so the
 * value itself needs no epoch of its own: it just has to move by one per local midnight.
 */
expect fun deviceDayNumber(epochMillis: Long): Long

expect fun deviceTimeOfDayLabel(epochMillis: Long): String

expect fun deviceWeekdayLabel(epochMillis: Long): String

expect fun deviceDateLabel(epochMillis: Long): String
