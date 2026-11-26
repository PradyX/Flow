package io.github.aedev.flow.utils

import java.time.Instant
import java.time.ZoneId

internal actual fun platformUtcOffsetMinutes(): Int =
    ZoneId
        .systemDefault()
        .rules
        .getOffset(Instant.now())
        .totalSeconds / 60
