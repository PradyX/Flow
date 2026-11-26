package io.github.aedev.flow.utils

/**
 * A count as the platform's own CLDR data abbreviates it in the device language ("1.2M", "2,4 Mio.",
 * "33万"). Android uses ICU's [android.icu.text.CompactDecimalFormat], which is what the app shipped
 * with; desktop and iOS have no such API on the shared classpath, so each target supplies its own.
 */
expect fun formatViewCount(count: Long): String

/** The premiere moment in the device's local time zone and locale. */
expect fun premiereDateText(epochMs: Long): String
