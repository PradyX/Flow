package io.github.aedev.flow.data.playback

import java.io.File

actual fun loadPlaybackSessionJson(): String? =
    runCatching { sessionFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun savePlaybackSessionJson(json: String) {
    runCatching {
        val file = sessionFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

actual fun clearPlaybackSessionJson() {
    runCatching { sessionFile().delete() }
}

private fun sessionFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-playback-session.json")
