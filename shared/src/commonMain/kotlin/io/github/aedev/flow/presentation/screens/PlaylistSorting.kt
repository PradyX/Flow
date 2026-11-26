package io.github.aedev.flow.presentation.screens

import io.github.aedev.flow.presentation.feed.FeedEntry

/**
 * How an album or playlist page orders its rows; the collection's own order is the default.
 * [storageValue] is what the page writes down, so the chip remembers the choice per collection.
 */
internal enum class PlaylistSortOrder(
    val storageValue: String,
) {
    COLLECTION("collection"),
    TITLE("title"),
    LONGEST("longest"),
    SHORTEST("shortest"),
    ;

    companion object {
        fun fromStorage(value: String?): PlaylistSortOrder = entries.firstOrNull { it.storageValue == value } ?: COLLECTION
    }
}

/**
 * [this] in [order]. Ties keep the collection's own order, so a sort never shuffles equal songs; a
 * row whose length YouTube did not send sorts last under "Shortest first" rather than first.
 */
internal fun List<FeedEntry>.sortedForPlaylist(order: PlaylistSortOrder): List<FeedEntry> =
    when (order) {
        PlaylistSortOrder.COLLECTION -> {
            this
        }

        PlaylistSortOrder.TITLE -> {
            sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        }

        PlaylistSortOrder.LONGEST -> {
            sortedByDescending { it.durationSeconds ?: 0 }
        }

        PlaylistSortOrder.SHORTEST -> {
            sortedWith(
                compareBy { entry ->
                    entry.durationSeconds?.takeIf { it > 0 } ?: Int.MAX_VALUE
                },
            )
        }
    }
