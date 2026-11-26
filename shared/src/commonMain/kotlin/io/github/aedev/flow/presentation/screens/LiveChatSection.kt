package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.model.LiveChatMessage
import io.github.aedev.flow.presentation.ui.ChannelAvatar
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.live_chat_member
import io.github.aedev.flow.resources.live_chat_moderator
import io.github.aedev.flow.resources.live_chat_owner
import io.github.aedev.flow.resources.live_chat_title
import io.github.aedev.flow.resources.live_chat_verified
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

private val LiveChatAvatarDiameter = 28
private val LiveChatRowSpacing = 8.dp

/**
 * A live stream's chat under the watch page: one row per message in YouTube's order, with the
 * author's photo, their badges and the message. Only a live stream has a chat token, so this
 * section never appears for a normal video.
 */
internal fun LazyListScope.liveChatSection(messages: List<LiveChatMessage>) {
    if (messages.isEmpty()) return
    item("live-chat-header") { LiveChatHeader() }
    items(messages, key = { "live-chat-${it.id}" }) { message ->
        LiveChatRow(message)
    }
}

@Composable
private fun LiveChatHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.ContentPaddingHorizontal, vertical = LiveChatRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.live_chat_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun LiveChatRow(message: LiveChatMessage) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.ContentPaddingHorizontal, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChannelAvatar(
            name = message.author,
            imageUrl = message.authorPhotoUrl,
            diameter = LiveChatAvatarDiameter,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.author,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                message.badgeLabel()?.let { badge ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                message.superChatAmount?.takeIf { it.isNotBlank() }?.let { amount ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LiveChatMessage.badgeLabel(): String? =
    when {
        isOwner -> stringResource(Res.string.live_chat_owner)
        isModerator -> stringResource(Res.string.live_chat_moderator)
        isMember -> stringResource(Res.string.live_chat_member)
        isVerified -> stringResource(Res.string.live_chat_verified)
        else -> null
    }
