package io.github.aedev.flow.utils

import platform.Foundation.NSTimeZone

internal actual fun platformUtcOffsetMinutes(): Int = (NSTimeZone.localTimeZone.secondsFromGMT / 60).toInt()
