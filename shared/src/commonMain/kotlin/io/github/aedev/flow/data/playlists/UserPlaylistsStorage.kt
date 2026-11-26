package io.github.aedev.flow.data.playlists

/** Where the viewer's playlists are kept between runs, as text any host can write. */
expect fun loadUserPlaylistsJson(): String?

expect fun saveUserPlaylistsJson(json: String)
