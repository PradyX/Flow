package io.github.aedev.flow.presentation.screens

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.aedev.flow.data.library.FlowLibraryDatabase
import io.github.aedev.flow.data.library.LibraryStore
import io.github.aedev.flow.data.library.openLibraryDatabase
import kotlinx.coroutines.Dispatchers
import java.nio.file.Files

/** A store over an in-memory database, for rendering tests that only need the surface. */
internal fun inMemoryLibraryStore(): LibraryStore =
    LibraryStore(
        Room
            .inMemoryDatabaseBuilder<FlowLibraryDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build(),
    )

/** A store over a database file, for the round-trip test's two instances. */
internal fun fileLibraryStore(path: String): LibraryStore = LibraryStore(openLibraryDatabase(path))

internal fun tempLibraryDatabasePath(): String =
    Files
        .createTempDirectory("flow-library")
        .resolve("library.db")
        .toString()
