package io.github.aedev.flow.data.notes

import platform.Foundation.NSUserDefaults

actual fun loadVideoNotesJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY)

actual fun saveVideoNotesJson(json: String) {
    NSUserDefaults.standardUserDefaults.setObject(json, KEY)
}

private const val KEY = "flow_video_notes"
