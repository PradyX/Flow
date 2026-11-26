package io.github.aedev.flow.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import kotlin.time.Instant

actual fun platformIODispatcher(): CoroutineDispatcher = Dispatchers.Default

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun localMinuteOfDay(instant: Instant): Int {
    val date = NSDate.dateWithTimeIntervalSince1970(instant.toEpochMilliseconds() / 1000.0)
    val components =
        NSCalendar.currentCalendar.components(
            NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = date,
        )
    return components.hour.toInt() * 60 + components.minute.toInt()
}
