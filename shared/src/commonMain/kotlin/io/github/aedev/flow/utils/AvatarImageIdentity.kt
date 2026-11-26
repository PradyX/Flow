package io.github.aedev.flow.utils

/**
 * Identity of an avatar image ignoring the size query and the crop parameters YouTube appends, so
 * the same channel's avatar at two sizes de-duplicates to one.
 */
internal fun String?.avatarImageIdentityKey(): String =
    orEmpty()
        .substringBefore("?")
        .replace(Regex("=s\\d+.*$"), "")
