package io.github.aedev.flow.utils

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun premiereDateText(epochMs: Long): String {
    val formatter =
        NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd HH:mm"
            locale = NSLocale.currentLocale
            timeZone = NSTimeZone.localTimeZone
        }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0))
}

/**
 * Foundation has no CLDR compact formatter, so iOS shows the grouped count until the shared
 * formatting layer lands. Android and desktop both abbreviate; this is the one target that does not
 * yet, and it is the only place where the port knowingly differs in output.
 */
actual fun formatViewCount(count: Long): String {
    val formatter = numberFormatterDecimal()
    return formatter.stringFromNumber(count) ?: count.toString()
}

private fun numberFormatterDecimal() =
    platform.Foundation.NSNumberFormatter().apply {
        numberStyle = platform.Foundation.NSNumberFormatterStyle.NSNumberFormatterDecimalStyle
    }
