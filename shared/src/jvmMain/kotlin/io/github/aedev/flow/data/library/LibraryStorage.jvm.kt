package io.github.aedev.flow.data.library

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun openLibraryDatabase(path: String): FlowLibraryDatabase =
    Room
        .databaseBuilder<FlowLibraryDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

actual fun openDefaultLibraryDatabase(): FlowLibraryDatabase {
    val directory = File(System.getProperty("user.home"), ".flow")
    directory.mkdirs()
    return openLibraryDatabase(File(directory, "flow-library.db").absolutePath)
}
