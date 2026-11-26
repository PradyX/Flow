package io.github.aedev.flow.utils

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** `android.icu` does not exist on desktop, so this uses the JDK's CLDR-backed compact instance. */
actual fun formatViewCount(count: Long): String =
    NumberFormat.getCompactNumberInstance(Locale.getDefault(), NumberFormat.Style.SHORT).format(count)

actual fun premiereDateText(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(PREMIERE_DATE_FORMAT)

private val PREMIERE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
