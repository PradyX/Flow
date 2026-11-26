package io.github.aedev.flow.data.local

import platform.Foundation.NSUserDefaults

actual fun loadLocalMediaJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun saveLocalMediaJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

private const val KEY = "flow_local_media"
