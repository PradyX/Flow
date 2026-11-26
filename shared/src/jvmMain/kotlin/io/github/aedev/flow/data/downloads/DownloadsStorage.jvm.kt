package io.github.aedev.flow.data.downloads

import java.io.File

actual fun loadDownloadsJson(): String? =
    runCatching { downloadsFile().takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() } }.getOrNull()

actual fun saveDownloadsJson(json: String) {
    runCatching {
        val file = downloadsFile()
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}

private fun downloadsFile(): File = File(File(System.getProperty("user.home"), ".flow"), "flow-downloads.json")
