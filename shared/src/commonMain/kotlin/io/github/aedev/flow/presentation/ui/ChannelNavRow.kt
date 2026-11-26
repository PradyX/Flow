package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.FeedEntry
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.artist_subscribers
import io.github.aedev.flow.utils.formatViewCount
import org.jetbrains.compose.resources.stringResource

private val ChannelRowInset = 12.dp
private const val CHANNEL_AVATAR_DIAMETER = 40

/** A channel result or shelf entry: its avatar, name and audience, opening the channel page. */
@Composable
fun ChannelNavRow(
    entry: FeedEntry,
    onOpenChannel: (channelId: String, name: String) -> Unit,
) {
    val channelId = entry.channelId ?: return
    val subscribers = entry.channelSubscriberCount?.let { stringResource(Res.string.artist_subscribers, formatViewCount(it)) }
    FlowNavRow(
        title = entry.title,
        onClick = { onOpenChannel(channelId, entry.title) },
        modifier = Modifier.padding(horizontal = ChannelRowInset),
        supportingText = listOfNotNull(subscribers, entry.subtitle).joinToString(" · ").takeIf { it.isNotBlank() },
        showChevron = true,
        leadingContent = {
            ChannelAvatar(
                name = entry.title,
                imageUrl = entry.thumbnailUrl,
                diameter = CHANNEL_AVATAR_DIAMETER,
            )
        },
    )
}
