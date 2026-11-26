package io.github.aedev.flow.presentation.nav

/**
 * Every screen the shared UI can show. Plain data, so the same values drive the composables and the
 * headless navigation test.
 */
sealed interface AppDestination {
    /**
     * Tab roots carry a stable name so a relaunch can reopen the one the last run left, the way the
     * app's restored navigation stack does. Detail pages have none: a relaunch does not reopen them.
     */
    val tabName: String?
        get() = null

    data object Home : AppDestination {
        override val tabName: String = "home"
    }

    data object Search : AppDestination {
        override val tabName: String = "search"
    }

    /** YouTube Music's shelves, the tab the app puts between Shorts and Subscriptions. */
    data object Music : AppDestination {
        override val tabName: String = "music"
    }

    /** The uploads of the channels setup subscribed to, a tab rather than a pushed page. */
    data object Subscriptions : AppDestination {
        override val tabName: String = "subscriptions"
    }

    /** The saved and downloaded library, a tab rather than a pushed detail page. */
    data object Library : AppDestination {
        override val tabName: String = "library"
    }

    /** The app's Categories page: one chip per YouTube destination, a tab of its own. */
    data object Explore : AppDestination {
        override val tabName: String = "explore"
    }

    /** The full-height reel feed, opened from the Home bar. */
    data object Shorts : AppDestination

    /** YouTube Music's moods and genres index, opened from the music tab's bar. */
    data object Moods : AppDestination

    /** One mood or genre's collection, opened from a button on the moods index. */
    data class Mood(
        val browseId: String,
        val params: String? = null,
        val title: String? = null,
    ) : AppDestination

    /** The videos this device watched, opened from the profile sheet and the library's bar. */
    data object History : AppDestination

    /** The notes written on videos, opened from the profile sheet and the player's action row. */
    data object Notes : AppDestination

    /** The playlists this device made, opened from the Library's bar and a player's action row. */
    data object Playlists : AppDestination

    /** One playlist the viewer made; the page reads the record the shell keeps. */
    data class LocalPlaylist(
        val playlistId: String,
    ) : AppDestination

    /** Appearance, the records the app keeps, and its build; opened from the profile sheet. */
    data object Settings : AppDestination

    /**
     * A shelf's See all from Categories, pushed over the tab that opened it. [params] is the token
     * the shelf's own response carried; [title] is the shelf's heading, so the page is never blank.
     */
    data class ExploreShelf(
        val browseId: String,
        val params: String,
        val title: String? = null,
    ) : AppDestination

    /** A channel page, opened from the player's channel row. */
    data class Channel(
        val channelId: String,
        val name: String,
    ) : AppDestination

    /**
     * A music artist's page. [name] and [thumbnailUrl] are what the card already knew, so the header
     * is never blank while the page loads.
     */
    data class Artist(
        val artistId: String,
        val name: String,
        val thumbnailUrl: String? = null,
    ) : AppDestination

    /**
     * A playlist page. [title] and [thumbnailUrl] are what the card already knew, so the header is
     * never blank while the page loads.
     */
    data class Playlist(
        val playlistId: String,
        val title: String,
        val thumbnailUrl: String? = null,
    ) : AppDestination

    /**
     * A community post's own page. [params] is the token the post's comment button carries, which
     * is the only form WEB answers the post page to; [authorName] titles the page while it loads.
     */
    data class Post(
        val postId: String,
        val params: String,
        val authorName: String,
    ) : AppDestination

    /**
     * A selected video. [title] and [thumbnailUrl] are what the list already knew, shown until the
     * detail arrives so the player opens with its poster rather than an empty frame.
     */
    data class Player(
        val videoId: String,
        val title: String,
        val thumbnailUrl: String? = null,
    ) : AppDestination
}

/** The tab a stored [name] reopens; null when nothing was stored or the name is not a tab's. */
fun restoredTab(name: String?): AppDestination? =
    listOf(
        AppDestination.Home,
        AppDestination.Music,
        AppDestination.Subscriptions,
        AppDestination.Library,
        AppDestination.Explore,
        AppDestination.Search,
    ).firstOrNull { it.tabName == name }
