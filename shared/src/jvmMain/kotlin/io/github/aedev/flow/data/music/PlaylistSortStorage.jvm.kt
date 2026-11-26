package io.github.aedev.flow.data.music

import java.io.File

actual fun loadPlaylistSortOrder(playlistId: String): String? =
    runCatching {
        sortOrders()
            .lines()
            .firstOrNull { it.substringBefore('=') == playlistId }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

actual fun savePlaylistSortOrder(
    playlistId: String,
    value: String,
) {
    runCatching {
        val kept = sortOrders().lines().filter { it.isNotBlank() && it.substringBefore('=') != playlistId }
        val file = sortOrdersFile()
        file.parentFile?.mkdirs()
        file.writeText((kept + "$playlistId=$value").joinToString("\n"))
    }
}

private fun sortOrders(): String = sortOrdersFile().takeIf { it.isFile }?.readText().orEmpty()

private fun sortOrdersFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-playlist-sorts.txt")
