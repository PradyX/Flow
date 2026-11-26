package io.github.aedev.flow.presentation.nav

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The stack a relaunch reopens: the tab root plus the pages pushed over it, each carrying what it
 * needs to open again. A page whose identifiers are gone ends the chain, so the relaunch lands on
 * the last page that could be reopened.
 *
 * The stored shape is this file's own, not [AppDestination]'s: a route that reads back after a
 * relaunch is a compatibility promise, so the field names here are the format.
 */
@Serializable
private data class StoredDestination(
    val tab: String? = null,
    /** A page with no identifier of its own, such as the reel feed. */
    val kind: String? = null,
    val channelId: String? = null,
    val channelName: String? = null,
    val playlistId: String? = null,
    val playlistTitle: String? = null,
    val playlistThumbnailUrl: String? = null,
    val videoId: String? = null,
    val videoTitle: String? = null,
    val videoThumbnailUrl: String? = null,
    val artistId: String? = null,
    val artistName: String? = null,
    val artistThumbnailUrl: String? = null,
)

private const val KIND_SHORTS = "shorts"

@Serializable
private data class StoredNavigation(
    val routes: List<StoredDestination>,
)

private val NavigationJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

/** The restorable prefix of [stack] as the text the storage seam keeps. */
fun navigationStateJson(stack: List<AppDestination>): String {
    val routes = mutableListOf<StoredDestination>()
    for (destination in stack) {
        routes += destination.toStored() ?: break
    }
    return NavigationJson.encodeToString(StoredNavigation.serializer(), StoredNavigation(routes))
}

/**
 * The stack [json] describes, or an empty list when nothing in it can be reopened. A stored tab name
 * from a build that only kept the tab still resolves; a name that is not a route, a route whose
 * identifiers are gone, or a stack that does not start on a tab root resolves to nothing, and the
 * caller falls back to Home.
 */
fun restoredNavigationStack(json: String?): List<AppDestination> {
    val text = json?.trim().orEmpty()
    if (text.isEmpty()) return emptyList()
    val routes =
        if (text.startsWith("{")) {
            runCatching { NavigationJson.decodeFromString(StoredNavigation.serializer(), text) }
                .getOrNull()
                ?.routes
        } else {
            listOf(StoredDestination(tab = text))
        } ?: return emptyList()
    val restored = mutableListOf<AppDestination>()
    for (route in routes) {
        restored += route.toDestination() ?: break
    }
    return if (restored.firstOrNull()?.tabName == null) emptyList() else restored
}

private fun AppDestination.toStored(): StoredDestination? =
    when (this) {
        is AppDestination.Channel -> {
            channelId.takeIf { it.isNotBlank() }?.let { StoredDestination(channelId = it, channelName = name) }
        }

        is AppDestination.Playlist -> {
            playlistId.takeIf { it.isNotBlank() }?.let {
                StoredDestination(playlistId = it, playlistTitle = title, playlistThumbnailUrl = thumbnailUrl)
            }
        }

        is AppDestination.Artist -> {
            artistId.takeIf { it.isNotBlank() }?.let {
                StoredDestination(artistId = it, artistName = name, artistThumbnailUrl = thumbnailUrl)
            }
        }

        is AppDestination.Shorts -> {
            StoredDestination(kind = KIND_SHORTS)
        }

        is AppDestination.Player -> {
            videoId.takeIf { it.isNotBlank() }?.let {
                StoredDestination(videoId = it, videoTitle = title, videoThumbnailUrl = thumbnailUrl)
            }
        }

        else -> {
            tabName?.let { StoredDestination(tab = it) }
        }
    }

private fun StoredDestination.toDestination(): AppDestination? {
    tab?.let { return restoredTab(it) }
    if (kind == KIND_SHORTS) return AppDestination.Shorts
    channelId?.takeIf { it.isNotBlank() }?.let { return AppDestination.Channel(it, channelName.orEmpty()) }
    playlistId?.takeIf { it.isNotBlank() }?.let {
        return AppDestination.Playlist(it, playlistTitle.orEmpty(), playlistThumbnailUrl)
    }
    artistId?.takeIf { it.isNotBlank() }?.let {
        return AppDestination.Artist(it, artistName.orEmpty(), artistThumbnailUrl)
    }
    videoId?.takeIf { it.isNotBlank() }?.let {
        return AppDestination.Player(it, videoTitle.orEmpty(), videoThumbnailUrl)
    }
    return null
}
