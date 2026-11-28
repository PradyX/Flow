package io.github.aedev.flow.data.notifications

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun loadNotificationInbox(): NotificationInbox? =
    NotificationInbox.decode(NSUserDefaults.standardUserDefaults.stringForKey(KEY_NOTIFICATIONS))

actual fun saveNotificationInbox(inbox: NotificationInbox) {
    NSUserDefaults.standardUserDefaults.setObject(inbox.encode(), KEY_NOTIFICATIONS)
}

actual fun deviceDayNumber(epochMillis: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val components = NSCalendar.currentCalendar.components(NSCalendarUnitDay, fromDate = date)
    // The day of the era is not what matters here, only that one local midnight moves it by one.
    return (date.timeIntervalSince1970 / 86_400.0).toLong() + components.day
}

actual fun deviceTimeOfDayLabel(epochMillis: Long): String = label(epochMillis, NSDateFormatterShortStyle, NSDateFormatterNoStyle)

actual fun deviceWeekdayLabel(epochMillis: Long): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "EEEE"
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

actual fun deviceDateLabel(epochMillis: Long): String = label(epochMillis, NSDateFormatterNoStyle, NSDateFormatterMediumStyle)

private fun label(
    epochMillis: Long,
    timeStyle: Long,
    dateStyle: Long,
): String {
    val formatter = NSDateFormatter()
    formatter.timeStyle = timeStyle
    formatter.dateStyle = dateStyle
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0))
}

private const val KEY_NOTIFICATIONS = "flow_notifications"
