package io.github.aedev.flow.data.library

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

private var applicationContext: Context? = null

/** Points the library database at the app's own files directory; called once from the host. */
fun initializeLibraryStorage(context: Context) {
    applicationContext = context.applicationContext
}

actual fun openLibraryDatabase(path: String): FlowLibraryDatabase {
    val context = applicationContext ?: error("initializeLibraryStorage was not called")
    return Room
        .databaseBuilder(context, FlowLibraryDatabase::class.java, path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(FlowLibraryDatabase.MIGRATION_1_2)
        .build()
}

actual fun openDefaultLibraryDatabase(): FlowLibraryDatabase {
    val directory = applicationContext?.filesDir ?: error("initializeLibraryStorage was not called")
    return openLibraryDatabase(File(directory, "flow-library.db").absolutePath)
}
