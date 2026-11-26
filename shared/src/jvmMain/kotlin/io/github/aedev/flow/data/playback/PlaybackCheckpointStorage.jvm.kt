package io.github.aedev.flow.data.playback

import java.io.File

actual fun loadPlaybackCheckpointJson(): String? =
    runCatching { checkpointFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun savePlaybackCheckpointJson(json: String) {
    runCatching {
        val file = checkpointFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

actual fun clearPlaybackCheckpointJson() {
    runCatching { checkpointFile().delete() }
}

private fun checkpointFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-player-checkpoint.json")
