package io.github.aedev.flow.data.local

import java.io.File

actual fun loadLocalMediaJson(): String? =
    runCatching { localMediaFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun saveLocalMediaJson(json: String) {
    runCatching {
        val file = localMediaFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun localMediaFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-local-media.json")
