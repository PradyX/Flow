package io.github.aedev.flow.core.common

import kotlin.time.Instant

/** Clock access for shared code — commonMain has no wall clock of its own. */
expect fun currentTimeMillis(): Long

/**
 * Minute of the day in the device's current timezone, for rules that reason
 * about local wall-clock time (quiet hours, reminder schedules) rather than UTC.
 */
expect fun localMinuteOfDay(instant: Instant): Int

fun nowInstant(): Instant = Instant.fromEpochMilliseconds(currentTimeMillis())
