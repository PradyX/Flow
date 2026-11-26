package io.github.aedev.flow.data.library

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * One saved video in the on-device library: the metadata the list needs to draw its row without a
 * network call, plus the flags the app's Library and Downloads surfaces group by.
 */
@Entity(tableName = "library_item")
data class LibraryItemEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelName: String?,
    val thumbnailUrl: String?,
    val isDownloaded: Boolean,
    val savedAtMillis: Long,
    val isShort: Boolean = false,
)

@Dao
interface LibraryItemDao {
    @Query("SELECT * FROM library_item ORDER BY savedAtMillis DESC")
    suspend fun all(): List<LibraryItemEntity>

    @Query("SELECT * FROM library_item WHERE videoId = :videoId")
    suspend fun find(videoId: String): LibraryItemEntity?

    @Upsert
    suspend fun upsert(item: LibraryItemEntity)

    @Query("DELETE FROM library_item WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}

@Database(entities = [LibraryItemEntity::class], version = 2, exportSchema = false)
abstract class FlowLibraryDatabase : RoomDatabase() {
    abstract fun libraryItems(): LibraryItemDao

    companion object {
        /** The saved-shorts flag arrived after the first release; existing rows are all videos. */
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(connection: SQLiteConnection) {
                    connection.execSQL("ALTER TABLE library_item ADD COLUMN isShort INTEGER NOT NULL DEFAULT 0")
                }
            }
    }
}

/** A saved video as the screens read it. */
data class LibraryItem(
    val videoId: String,
    val title: String,
    val channelName: String?,
    val thumbnailUrl: String?,
    val isDownloaded: Boolean,
    val isShort: Boolean = false,
)

private fun LibraryItemEntity.toItem(): LibraryItem =
    LibraryItem(
        videoId = videoId,
        title = title,
        channelName = channelName,
        thumbnailUrl = thumbnailUrl,
        isDownloaded = isDownloaded,
        isShort = isShort,
    )

/**
 * The library's saved and downloaded state, over Room. A video is saved or downloaded per row, so
 * one store serves both the Library list and the Downloads filter, and every write is on disk before
 * the call returns.
 */
class LibraryStore(
    private val database: FlowLibraryDatabase,
) {
    private val dao get() = database.libraryItems()

    suspend fun items(): List<LibraryItem> = dao.all().map(LibraryItemEntity::toItem)

    /** The saved rows that came from the reel, the collection the Saved Shorts page draws. */
    suspend fun savedShorts(): List<LibraryItem> = items().filter { it.isShort }

    suspend fun find(videoId: String): LibraryItem? = dao.find(videoId)?.toItem()

    /** Saves the video, or removes it when it is already saved; returns whether it is saved now. */
    suspend fun toggleSaved(
        videoId: String,
        title: String,
        channelName: String?,
        thumbnailUrl: String?,
        nowMillis: Long,
        isShort: Boolean = false,
    ): Boolean {
        val existing = dao.find(videoId)
        if (existing != null) {
            dao.delete(videoId)
            return false
        }
        dao.upsert(
            LibraryItemEntity(
                videoId = videoId,
                title = title,
                channelName = channelName,
                thumbnailUrl = thumbnailUrl,
                isDownloaded = false,
                savedAtMillis = nowMillis,
                isShort = isShort,
            ),
        )
        return true
    }

    /**
     * Downloads the video, or removes the download when it has one. A download is also a save, so a
     * video that was not in the library before is inserted with both flags.
     */
    suspend fun toggleDownloaded(
        videoId: String,
        title: String,
        channelName: String?,
        thumbnailUrl: String?,
        nowMillis: Long,
    ): Boolean {
        val existing = dao.find(videoId)
        if (existing != null) {
            if (existing.isDownloaded) {
                dao.delete(videoId)
                return false
            }
            dao.upsert(existing.copy(isDownloaded = true))
            return true
        }
        dao.upsert(
            LibraryItemEntity(
                videoId = videoId,
                title = title,
                channelName = channelName,
                thumbnailUrl = thumbnailUrl,
                isDownloaded = true,
                savedAtMillis = nowMillis,
            ),
        )
        return true
    }

    suspend fun setDownloaded(
        videoId: String,
        downloaded: Boolean,
    ) {
        val existing = dao.find(videoId) ?: return
        dao.upsert(existing.copy(isDownloaded = downloaded))
    }

    /**
     * Records that this video's media is on disk, adding its row when the video was never saved,
     * so a download always has somewhere to show up.
     */
    suspend fun markDownloaded(
        videoId: String,
        title: String,
        channelName: String?,
        thumbnailUrl: String?,
        nowMillis: Long,
    ) {
        val existing = dao.find(videoId)
        val row =
            existing ?: LibraryItemEntity(
                videoId = videoId,
                title = title,
                channelName = channelName,
                thumbnailUrl = thumbnailUrl,
                isDownloaded = false,
                savedAtMillis = nowMillis,
            )
        dao.upsert(
            row.copy(
                title = title.ifBlank { row.title },
                channelName = channelName ?: row.channelName,
                thumbnailUrl = thumbnailUrl ?: row.thumbnailUrl,
                isDownloaded = true,
            ),
        )
    }

    /** The media is gone: the row keeps standing as a save when it was saved as well. */
    suspend fun clearDownloaded(videoId: String) {
        val existing = dao.find(videoId) ?: return
        dao.upsert(existing.copy(isDownloaded = false))
    }

    suspend fun remove(videoId: String) {
        dao.delete(videoId)
    }

    /** Closes the underlying database, flushing anything the driver still holds. */
    fun close() {
        database.close()
    }
}

/** The library's database at [path]: a file path on every target. */
expect fun openLibraryDatabase(path: String): FlowLibraryDatabase

/**
 * The database the app opens on this platform. Android supplies its files directory through
 * [initializeLibraryStorage]; the desktop and iOS targets resolve it from the user's home.
 */
expect fun openDefaultLibraryDatabase(): FlowLibraryDatabase
