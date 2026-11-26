package io.github.aedev.flow.utils

/**
 * The device's current UTC offset in minutes.
 *
 * The WEB channel client must send this: channel rows carry server-rendered times ("Scheduled for
 * 9/16/26, 6:45 PM") and the client's zero offset would print them in UTC. `ZoneId.systemDefault()`
 * and `NSTimeZone` have no common API, so this is the seam.
 */
internal expect fun platformUtcOffsetMinutes(): Int
