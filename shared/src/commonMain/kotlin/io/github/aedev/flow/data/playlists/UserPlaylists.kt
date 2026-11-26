package io.github.aedev.flow.data.playlists

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One video on a playlist this device keeps: what its row draws, with no network call. */
@Serializable
data class PlaylistVideo(
    val videoId: String,
    val title: String,
    val channelName: String? = null,
    val thumbnailUrl: String? = null,
    val addedAtMillis: Long = 0,
)

/** A playlist the viewer made: its videos in the order they were added, newest playlist first. */
@Serializable
data class UserPlaylist(
    val id: String,
    val name: String,
    val videos: List<PlaylistVideo> = emptyList(),
    val createdAtMillis: Long = 0,
    val updatedAtMillis: Long = 0,
)

/**
 * Every playlist on the device. Create, rename, delete and the video list are pure operations on
 * the document, so the screens read one value and the host writes the one it is handed.
 */
@Serializable
data class UserPlaylists(
    val playlists: List<UserPlaylist> = emptyList(),
) {
    fun find(id: String): UserPlaylist? = playlists.firstOrNull { it.id == id }

    fun contains(
        id: String,
        videoId: String,
    ): Boolean = find(id)?.videos?.any { it.videoId == videoId } == true

    /** Adds [name] as a new playlist at the top; a blank name is not a playlist. */
    fun create(
        name: String,
        nowMillis: Long,
    ): UserPlaylists {
        val title = name.trim()
        if (title.isEmpty() || playlists.size >= MAX_PLAYLISTS) return this
        val playlist =
            UserPlaylist(
                id = freshId(nowMillis),
                name = title,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
            )
        return UserPlaylists(listOf(playlist) + playlists)
    }

    fun rename(
        id: String,
        name: String,
        nowMillis: Long,
    ): UserPlaylists {
        val title = name.trim()
        if (title.isEmpty()) return this
        return UserPlaylists(
            playlists.map { playlist ->
                if (playlist.id == id) playlist.copy(name = title, updatedAtMillis = nowMillis) else playlist
            },
        )
    }

    fun delete(id: String): UserPlaylists = UserPlaylists(playlists.filterNot { it.id == id })

    /** Puts [video] on the playlist, unless it is already there: a video is on a playlist once. */
    fun add(
        id: String,
        video: PlaylistVideo,
        nowMillis: Long,
    ): UserPlaylists {
        if (video.videoId.isBlank()) return this
        return UserPlaylists(
            playlists.map { playlist ->
                when {
                    playlist.id != id -> {
                        playlist
                    }

                    contains(id, video.videoId) -> {
                        playlist
                    }

                    playlist.videos.size >= MAX_VIDEOS -> {
                        playlist
                    }

                    else -> {
                        playlist.copy(
                            videos = playlist.videos + video.copy(addedAtMillis = nowMillis),
                            updatedAtMillis = nowMillis,
                        )
                    }
                }
            },
        )
    }

    fun remove(
        id: String,
        videoId: String,
        nowMillis: Long,
    ): UserPlaylists =
        UserPlaylists(
            playlists.map { playlist ->
                if (playlist.id == id) {
                    playlist.copy(
                        videos = playlist.videos.filterNot { it.videoId == videoId },
                        updatedAtMillis = nowMillis,
                    )
                } else {
                    playlist
                }
            },
        )

    fun encode(): String = PlaylistsJson.encodeToString(serializer(), this)

    private fun freshId(nowMillis: Long): String {
        val base = "local-$nowMillis"
        if (playlists.none { it.id == base }) return base
        var suffix = 2
        while (playlists.any { it.id == "$base-$suffix" }) suffix++
        return "$base-$suffix"
    }

    companion object {
        const val MAX_PLAYLISTS = 100
        const val MAX_VIDEOS = 500

        fun decode(json: String?): UserPlaylists {
            val text = json?.takeIf { it.isNotBlank() } ?: return UserPlaylists()
            val parsed = runCatching { PlaylistsJson.decodeFromString(serializer(), text) }.getOrNull()
            return parsed ?: UserPlaylists()
        }
    }
}

private val PlaylistsJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
