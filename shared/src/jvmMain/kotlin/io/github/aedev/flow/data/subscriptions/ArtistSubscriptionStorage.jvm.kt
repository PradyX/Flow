package io.github.aedev.flow.data.subscriptions

import java.io.File

actual fun loadSubscribedArtistIds(): Set<String> =
    runCatching {
        subscribedArtistsFile()
            .takeIf { it.isFile }
            ?.readLines()
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
    }.getOrDefault(emptySet())

actual fun saveSubscribedArtistIds(ids: Set<String>) {
    runCatching {
        val file = subscribedArtistsFile()
        file.parentFile?.mkdirs()
        file.writeText(ids.joinToString("\n"))
    }
}

private fun subscribedArtistsFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-artist-subscriptions.txt")
