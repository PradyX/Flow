package io.github.aedev.flow.data.library

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSHomeDirectory

actual fun openLibraryDatabase(path: String): FlowLibraryDatabase =
    Room
        .databaseBuilder<FlowLibraryDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

actual fun openDefaultLibraryDatabase(): FlowLibraryDatabase = openLibraryDatabase(NSHomeDirectory() + "/flow-library.db")
