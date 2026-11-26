package io.github.aedev.flow.data.playback

import android.content.Context
import java.io.File

private var checkpointDirectory: File? = null

/** Points the checkpoint store at the app's own files; called once from the host. */
fun initializePlaybackCheckpointStorage(context: Context) {
    checkpointDirectory = context.applicationContext.filesDir
}

actual fun loadPlaybackCheckpointJson(): String? =
    runCatching { checkpointFile()?.takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

/**
 * Written synchronously: the process can be killed without warning, and a record still in a Java
 * buffer would be lost while one handed to the kernel is not. The record is a few dozen bytes.
 */
actual fun savePlaybackCheckpointJson(json: String) {
    runCatching {
        val file = checkpointFile() ?: return
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

actual fun clearPlaybackCheckpointJson() {
    runCatching { checkpointFile()?.delete() }
}

private fun checkpointFile(): File? = checkpointDirectory?.let { File(it, "player_checkpoint.json") }
