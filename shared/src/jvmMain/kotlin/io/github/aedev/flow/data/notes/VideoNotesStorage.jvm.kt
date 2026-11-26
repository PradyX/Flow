package io.github.aedev.flow.data.notes

import java.io.File

actual fun loadVideoNotesJson(): String? =
    runCatching { notesFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun saveVideoNotesJson(json: String) {
    runCatching {
        val file = notesFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun notesFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-video-notes.json")
