package io.github.aedev.flow.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowNavRow
import io.github.aedev.flow.presentation.ui.FlowSwitchRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.onboarding_channels_subscribe
import io.github.aedev.flow.resources.onboarding_channels_subscribed
import io.github.aedev.flow.resources.onboarding_import_count
import io.github.aedev.flow.resources.onboarding_import_count_one
import org.jetbrains.compose.resources.stringResource

internal val SegmentedGap = 2.dp
private val AvatarSize = 44.dp
private val SectionHeaderSpacing = 16.dp
private val RowPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
private val TileInset = 16.dp
private val GlyphSize = 40.dp
private val DoneSize = 24.dp
private val DoneInset = 10.dp

/** The setup's search box: leading glyph, the query, and a spinner while the search runs. */
@Composable
internal fun OnboardingSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    searching: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(FlowIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (searching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth(),
    )
}

/** An uppercase section label, as the channel results and artist lists head themselves. */
@Composable
internal fun OnboardingSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = SectionHeaderSpacing),
    )
}

/** A centred line of guidance where a list would be. */
@Composable
internal fun OnboardingPrompt(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
    )
}

/** A channel to subscribe to, with its bell once subscribed, as a slice of its segmented group. */
@Composable
internal fun ChannelRow(
    channel: OnboardingChannel,
    subscribed: Boolean,
    notifying: Boolean,
    onToggle: () -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    shape: Shape = MaterialTheme.shapes.large,
) {
    FlowNavRow(
        title = channel.name,
        supportingText = channel.subscriberCountText,
        onClick = onToggle,
        shape = shape,
        showChevron = false,
        leadingContent = { Avatar(channel.thumbnailUrl, channel.name) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onToggle) {
                    Text(
                        stringResource(
                            if (subscribed) Res.string.onboarding_channels_subscribed else Res.string.onboarding_channels_subscribe,
                        ),
                    )
                }
                if (subscribed) {
                    FilledTonalIconButton(onClick = { onNotificationsChange(!notifying) }) {
                        Icon(
                            FlowIcons.Bell,
                            contentDescription = null,
                            tint = if (notifying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

/** An artist with an add/added toggle, as a slice of its segmented group. */
@Composable
internal fun ArtistRow(
    artist: OnboardingArtist,
    picked: Boolean,
    onToggle: () -> Unit,
    shape: Shape = MaterialTheme.shapes.large,
) {
    FlowNavRow(
        title = artist.name,
        onClick = onToggle,
        shape = shape,
        showChevron = false,
        leadingContent = { Avatar(artist.thumbnailUrl, artist.name) },
        trailingContent = {
            FilledTonalIconToggleButton(checked = picked, onCheckedChange = { onToggle() }) {
                Icon(if (picked) FlowIcons.Check else FlowIcons.Add, contentDescription = null)
            }
        },
    )
}

/** A setting row that opens something else. */
@Composable
internal fun OnboardingNavRow(
    title: String,
    supportingText: String?,
    icon: ImageVector,
    showChevron: Boolean,
    onClick: () -> Unit,
    shape: Shape = MaterialTheme.shapes.large,
) {
    FlowNavRow(
        title = title,
        supportingText = supportingText,
        leadingIcon = icon,
        showChevron = showChevron,
        onClick = onClick,
        shape = shape,
    )
}

/** A setting row with a switch. */
@Composable
internal fun OnboardingSwitchRow(
    title: String,
    supportingText: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape = MaterialTheme.shapes.large,
) {
    FlowSwitchRow(
        title = title,
        supportingText = supportingText,
        leadingIcon = icon,
        checked = checked,
        onCheckedChange = onCheckedChange,
        shape = shape,
    )
}

/** A source app's tile: its glyph, its name, what it can import, and a check once imported. */
@Composable
internal fun ImportTile(
    title: String,
    kindCount: Int,
    imported: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxWidth().padding(TileInset),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(GlyphSize)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(FlowIcons.Inbox, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text =
                            stringResource(
                                if (kindCount ==
                                    1
                                ) {
                                    Res.string.onboarding_import_count_one
                                } else {
                                    Res.string.onboarding_import_count
                                },
                                kindCount,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (imported) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(DoneInset).size(DoneSize),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(FlowIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/** One import a source offers, in the sheet that lists them. */
@Composable
internal fun ImportKindRow(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(RowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(FlowIcons.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Avatar(
    url: String?,
    fallback: String,
) {
    val modifier =
        Modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    if (url.isNullOrBlank()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(fallback.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}
