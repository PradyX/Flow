package io.github.aedev.flow.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.ZoneId
import kotlin.time.Instant
import java.time.Instant as JavaInstant

actual fun platformIODispatcher(): CoroutineDispatcher = Dispatchers.IO

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun localMinuteOfDay(instant: Instant): Int {
    val zoned = JavaInstant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(ZoneId.systemDefault())
    return zoned.hour * 60 + zoned.minute
}
