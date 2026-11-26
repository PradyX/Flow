package io.github.aedev.flow.data.downloads

import platform.Foundation.NSUserDefaults

actual fun loadDownloadsJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun saveDownloadsJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

private const val KEY = "flow_downloads"
