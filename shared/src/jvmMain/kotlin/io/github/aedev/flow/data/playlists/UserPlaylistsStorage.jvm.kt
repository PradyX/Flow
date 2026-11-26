package io.github.aedev.flow.data.playlists

import java.io.File

actual fun loadUserPlaylistsJson(): String? =
    runCatching { playlistsFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun saveUserPlaylistsJson(json: String) {
    runCatching {
        val file = playlistsFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun playlistsFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-playlists.json")
