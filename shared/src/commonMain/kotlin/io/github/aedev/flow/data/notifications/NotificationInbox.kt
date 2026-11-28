package io.github.aedev.flow.data.notifications

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One row of the inbox: an upload the Subscriptions tab found after the channel's last check. */
@Serializable
data class InboxNotification(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String? = null,
    /** When the upload was announced, which is what the row's time line counts from. */
    val arrivedAtMillis: Long = 0,
    val isRead: Boolean = false,
)

/** One upload of a channel, as much as a row needs. */
data class InboxUpload(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String? = null,
)

/** A subscribed channel's uploads at the moment its feed was read, newest first. */
data class ChannelUploads(
    val channelId: String,
    val channelName: String,
    val uploads: List<InboxUpload>,
)

/**
 * The inbox the bar's bell opens: one row per upload the port had not seen yet, plus the newest
 * upload each channel had when it was last read.
 *
 * The app's subscription check announces the entries a channel published since the last video it
 * recorded and stores the newest one as the new pointer; a channel never checked before only
 * records its pointer, so a first visit does not mail out its whole back catalogue. The port has no
 * RSS check, so the Subscriptions tab's own refresh is the check: what that page reads is what this
 * record is told about.
 */
@Serializable
data class NotificationInbox(
    val entries: List<InboxNotification> = emptyList(),
    /** The newest upload each channel had when its feed was last read, by channel id. */
    val lastSeen: Map<String, String> = emptyMap(),
) {
    val unreadCount: Int get() = entries.count { !it.isRead }

    /**
     * The record after reading [channels]: rows for the uploads published since each channel's
     * pointer, and the pointer moved to the newest upload of every channel read. Stable when
     * nothing moved, so a repeated refresh changes neither the rows nor the stored pointers.
     */
    fun observed(
        channels: List<ChannelUploads>,
        nowMillis: Long,
    ): NotificationInbox {
        val seen = lastSeen.toMutableMap()
        val announced = mutableListOf<InboxNotification>()
        channels.forEach { channel ->
            val uploads = channel.uploads.filter { it.videoId.isNotBlank() && !it.videoId.startsWith("UC") }
            val newest = uploads.firstOrNull()?.videoId
            if (newest == null) {
                seen.remove(channel.channelId)
                return@forEach
            }
            val previous = seen[channel.channelId]
            seen[channel.channelId] = newest
            if (previous == null || previous == newest) return@forEach
            val knownIndex = uploads.indexOfFirst { it.videoId == previous }
            val fresh =
                when {
                    knownIndex > 0 -> uploads.take(minOf(knownIndex, MAX_PER_CHANNEL))

                    knownIndex == 0 -> emptyList()

                    // The pointer fell off the page: the channel's newest upload is what is news.
                    else -> uploads.take(1)
                }
            fresh.forEach { upload ->
                announced +=
                    InboxNotification(
                        videoId = upload.videoId,
                        title = upload.title,
                        channelName = channel.channelName,
                        thumbnailUrl = upload.thumbnailUrl,
                        arrivedAtMillis = nowMillis,
                    )
            }
        }
        val rows =
            (announced + entries)
                .distinctBy { it.videoId }
                .take(MAX_ENTRIES)
        return if (seen == lastSeen && rows.size == entries.size && announced.isEmpty()) this else NotificationInbox(rows, seen)
    }

    /** Opening the inbox marks what it shows as read, the way the app clears its badge. */
    fun markAllRead(): NotificationInbox = NotificationInbox(entries.map { it.copy(isRead = true) }, lastSeen)

    fun without(videoId: String): NotificationInbox = NotificationInbox(entries.filterNot { it.videoId == videoId }, lastSeen)

    /** Undo of a removal: the row goes back where it was, already read, as the app restores it. */
    fun restoring(
        notification: InboxNotification,
        index: Int,
    ): NotificationInbox {
        val remaining = entries.filterNot { it.videoId == notification.videoId }
        val at = index.coerceIn(0, remaining.size)
        return NotificationInbox(remaining.take(at) + notification.copy(isRead = true) + remaining.drop(at), lastSeen)
    }

    fun cleared(): NotificationInbox = NotificationInbox(emptyList(), lastSeen)

    fun encode(): String = InboxJson.encodeToString(serializer(), this)

    companion object {
        const val MAX_ENTRIES = 100

        /** The app caps a channel's backlog at ten rows per check. */
        const val MAX_PER_CHANNEL = 10

        private val InboxJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        fun decode(json: String?): NotificationInbox {
            val text = json?.takeIf { it.isNotBlank() } ?: return NotificationInbox()
            return runCatching { InboxJson.decodeFromString(serializer(), text) }.getOrNull() ?: NotificationInbox()
        }
    }
}

/** Where the inbox lives between launches, beside the history and the subscription record. */
expect fun loadNotificationInbox(): NotificationInbox?

expect fun saveNotificationInbox(inbox: NotificationInbox)
