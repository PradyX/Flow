package io.github.aedev.flow.presentation.onboarding

import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.import_engine_data
import io.github.aedev.flow.resources.import_engine_data_desc
import io.github.aedev.flow.resources.import_flow_backup_desc
import io.github.aedev.flow.resources.import_flow_backup_item_title
import io.github.aedev.flow.resources.import_freetube_history
import io.github.aedev.flow.resources.import_freetube_history_desc
import io.github.aedev.flow.resources.import_from_libretube
import io.github.aedev.flow.resources.import_from_libretube_desc
import io.github.aedev.flow.resources.import_from_metrolist
import io.github.aedev.flow.resources.import_from_metrolist_desc
import io.github.aedev.flow.resources.import_from_newpipe
import io.github.aedev.flow.resources.import_from_newpipe_desc
import io.github.aedev.flow.resources.import_from_youtube
import io.github.aedev.flow.resources.import_from_youtube_desc
import io.github.aedev.flow.resources.import_libretube_playlists
import io.github.aedev.flow.resources.import_libretube_playlists_desc
import io.github.aedev.flow.resources.import_master_backup_desc
import io.github.aedev.flow.resources.import_master_backup_title
import io.github.aedev.flow.resources.import_music_brain_desc
import io.github.aedev.flow.resources.import_music_brain_title
import io.github.aedev.flow.resources.import_newpipe_history
import io.github.aedev.flow.resources.import_newpipe_history_desc
import io.github.aedev.flow.resources.import_newpipe_playlists
import io.github.aedev.flow.resources.import_newpipe_playlists_desc
import io.github.aedev.flow.resources.import_yt_likes
import io.github.aedev.flow.resources.import_yt_likes_desc
import io.github.aedev.flow.resources.import_yt_music_playlist
import io.github.aedev.flow.resources.import_yt_music_playlist_desc
import io.github.aedev.flow.resources.import_yt_playlist
import io.github.aedev.flow.resources.import_yt_playlist_desc
import io.github.aedev.flow.resources.import_yt_takeout_all
import io.github.aedev.flow.resources.import_yt_takeout_all_desc
import io.github.aedev.flow.resources.import_yt_watch_history
import io.github.aedev.flow.resources.import_yt_watch_history_desc
import io.github.aedev.flow.resources.import_yt_watch_later
import io.github.aedev.flow.resources.import_yt_watch_later_desc
import io.github.aedev.flow.resources.onboarding_import_source_flow
import io.github.aedev.flow.resources.onboarding_import_source_youtube
import io.github.aedev.flow.resources.settings_backup_source_freetube
import io.github.aedev.flow.resources.settings_backup_source_libretube
import io.github.aedev.flow.resources.settings_backup_source_metrolist
import io.github.aedev.flow.resources.settings_backup_source_newpipe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.StringResource

private const val JSON = "application/json"
private const val ZIP = "application/zip"
private const val BINARY = "application/octet-stream"
private const val ANY = "*/*"
private val CsvTypes = listOf("text/comma-separated-values", "text/csv", "text/plain")
private val ZipTypes = listOf(ZIP, BINARY, ANY)

/**
 * Something the setup can import, with the file types the platform picker is offered. Mirrors the
 * app's `ImportKind`: same order inside a source, same descriptions.
 */
internal enum class ImportKind(
    val title: StringResource,
    val description: StringResource,
    val mimeTypes: List<String>,
    val takesSeveralFiles: Boolean = false,
) {
    FlowBackup(Res.string.import_flow_backup_item_title, Res.string.import_flow_backup_desc, listOf(JSON)),
    Master(Res.string.import_master_backup_title, Res.string.import_master_backup_desc, listOf(ZIP, BINARY)),
    Engine(Res.string.import_engine_data, Res.string.import_engine_data_desc, listOf(JSON)),
    MusicBrain(Res.string.import_music_brain_title, Res.string.import_music_brain_desc, listOf(JSON)),
    Takeout(Res.string.import_yt_takeout_all, Res.string.import_yt_takeout_all_desc, ZipTypes, takesSeveralFiles = true),
    YoutubeSubscriptions(Res.string.import_from_youtube, Res.string.import_from_youtube_desc, CsvTypes),
    YoutubeHistory(Res.string.import_yt_watch_history, Res.string.import_yt_watch_history_desc, listOf("text/html", "text/plain", ANY)),
    YoutubeLikes(Res.string.import_yt_likes, Res.string.import_yt_likes_desc, listOf(JSON, ZIP, BINARY, ANY)),
    YoutubePlaylist(Res.string.import_yt_playlist, Res.string.import_yt_playlist_desc, CsvTypes),
    WatchLater(Res.string.import_yt_watch_later, Res.string.import_yt_watch_later_desc, CsvTypes),
    YoutubeMusicPlaylist(Res.string.import_yt_music_playlist, Res.string.import_yt_music_playlist_desc, CsvTypes),
    NewPipeSubscriptions(Res.string.import_from_newpipe, Res.string.import_from_newpipe_desc, listOf(JSON)),
    NewPipeHistory(
        Res.string.import_newpipe_history,
        Res.string.import_newpipe_history_desc,
        listOf(ZIP, BINARY, "application/x-sqlite3", ANY),
    ),
    NewPipePlaylists(Res.string.import_newpipe_playlists, Res.string.import_newpipe_playlists_desc, listOf(ZIP, BINARY, ANY)),
    LibreTubeSubscriptions(Res.string.import_from_libretube, Res.string.import_from_libretube_desc, listOf(JSON)),
    LibreTubePlaylists(Res.string.import_libretube_playlists, Res.string.import_libretube_playlists_desc, listOf(JSON)),
    FreeTubeHistory(Res.string.import_freetube_history, Res.string.import_freetube_history_desc, listOf(JSON)),
    Metrolist(Res.string.import_from_metrolist, Res.string.import_from_metrolist_desc, listOf(ZIP, BINARY, ANY)),
}

/** The apps a person can move their data from, each with the imports it offers. */
internal enum class ImportSource(
    val shortTitle: StringResource,
    val kinds: List<ImportKind>,
) {
    Flow(
        Res.string.onboarding_import_source_flow,
        listOf(ImportKind.FlowBackup, ImportKind.Master, ImportKind.Engine, ImportKind.MusicBrain),
    ),
    Youtube(
        Res.string.onboarding_import_source_youtube,
        listOf(
            ImportKind.Takeout,
            ImportKind.YoutubeSubscriptions,
            ImportKind.YoutubeHistory,
            ImportKind.YoutubeLikes,
            ImportKind.YoutubePlaylist,
            ImportKind.WatchLater,
            ImportKind.YoutubeMusicPlaylist,
        ),
    ),
    NewPipe(
        Res.string.settings_backup_source_newpipe,
        listOf(ImportKind.NewPipeSubscriptions, ImportKind.NewPipeHistory, ImportKind.NewPipePlaylists),
    ),
    LibreTube(
        Res.string.settings_backup_source_libretube,
        listOf(ImportKind.LibreTubeSubscriptions, ImportKind.LibreTubePlaylists),
    ),
    FreeTube(Res.string.settings_backup_source_freetube, listOf(ImportKind.FreeTubeHistory)),
    Metrolist(Res.string.settings_backup_source_metrolist, listOf(ImportKind.Metrolist)),
}

/**
 * Reads a NewPipe subscriptions export: `{"subscriptions":[{"url":"https://www.youtube.com/channel/UC…","name":"…","service_id":0}]}`.
 * Other services' entries are skipped, as the app skips them.
 */
internal fun parseNewPipeSubscriptions(text: String): List<OnboardingChannel> {
    val root = runCatching { Json.parseToJsonElement(text) }.getOrNull()
    val entries =
        when (root) {
            is JsonArray -> root
            is JsonObject -> root["subscriptions"] as? JsonArray
            else -> null
        } ?: return emptyList()
    return entries.mapNotNull { element ->
        val entry = element as? JsonObject ?: return@mapNotNull null
        val service = entry["service_id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        if (service != 0) return@mapNotNull null
        val url = entry["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val id = url.substringAfterLast("/").substringBefore("?").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        OnboardingChannel(
            id = id,
            name = entry["name"]?.jsonPrimitive?.content ?: id,
            thumbnailUrl = null,
        )
    }
}
