package io.github.aedev.flow.utils

import android.icu.text.CompactDecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

actual fun formatViewCount(count: Long): String =
    CompactDecimalFormat
        .getInstance(
            Locale.getDefault(),
            CompactDecimalFormat.CompactStyle.SHORT,
        ).format(count)

actual fun premiereDateText(epochMs: Long): String =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(PREMIERE_DATE_FORMAT)

private val PREMIERE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
