package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.innertube.pages.channel.ChannelFilterGroup
import io.github.aedev.flow.presentation.feed.ChannelFeed
import io.github.aedev.flow.presentation.feed.chosenIndex
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.channel_about_links
import io.github.aedev.flow.resources.channel_about_more_info
import io.github.aedev.flow.resources.channel_tab_about
import org.jetbrains.compose.resources.stringResource

private val TabHeight = 44.dp
private val TabIndicatorHeight = 2.dp
private val TabDividerThickness = 0.5.dp
private val FilterRowInset = 4.dp
private val FilterRowBottomPadding = 8.dp
private val FilterChipSpacing = 8.dp
private val FilterChipContentPadding = PaddingValues(horizontal = 8.dp)
private val AboutPadding = 16.dp
private val AboutSectionSpacing = 20.dp
private val AboutBlockSpacing = 8.dp
private val AboutRowSpacing = 14.dp

/**
 * The channel's tab row, as the app draws it: a scrollable secondary tab row on the page's own
 * background, a 2dp primary indicator under the open tab and a hairline rule beneath the row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChannelTabRow(
    selectedIndex: Int,
    titles: List<String>,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedIndex),
                    height = TabIndicatorHeight,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            divider = {},
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    modifier = Modifier.height(TabHeight),
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = TabDividerThickness,
        )
    }
}

/**
 * A tab's filter bar. The sort control opens a menu, matching YouTube's own control, and the
 * remaining filters are chips; drawing the menu as chips would present five options of two
 * different kinds as one row of switches.
 */
@Composable
internal fun ChannelFilterBar(
    filters: List<ChannelFilterGroup>,
    picks: List<Int>,
    onSelect: (group: Int, option: Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = FilterRowInset, end = FilterRowInset, bottom = FilterRowBottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            contentPadding = FilterChipContentPadding,
            horizontalArrangement = Arrangement.spacedBy(FilterChipSpacing),
        ) {
            filters.forEachIndexed { groupIndex, group ->
                if (group.isDropdown) {
                    item(key = "filter_$groupIndex") {
                        ChannelFilterDropdown(
                            group = group,
                            chosen = group.chosenIndex(picks.getOrNull(groupIndex)),
                            onSelected = { option -> onSelect(groupIndex, option) },
                        )
                    }
                } else {
                    itemsIndexed(
                        items = group.options,
                        key = { optionIndex, _ -> "filter_${groupIndex}_$optionIndex" },
                    ) { optionIndex, option ->
                        FilterChip(
                            selected = group.chosenIndex(picks.getOrNull(groupIndex)) == optionIndex,
                            onClick = { onSelect(groupIndex, optionIndex) },
                            label = { Text(text = option.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelFilterDropdown(
    group: ChannelFilterGroup,
    chosen: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label =
        group.title ?: group.options
            .getOrNull(chosen)
            ?.label
            .orEmpty()

    Box {
        FilterChip(
            selected = group.options.getOrNull(chosen)?.selected == true,
            onClick = { expanded = true },
            label = { Text(text = label, maxLines = 1) },
            trailingIcon = {
                Icon(
                    imageVector = FlowIcons.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            group.options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(text = option.label) },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    },
                    leadingIcon =
                        if (index == chosen) {
                            { Icon(imageVector = FlowIcons.Check, contentDescription = null) }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

/**
 * The About panel: the description, then the channel's links, then every fact the About response
 * carried. A block is drawn only when it has something to show, as in the app.
 */
@Composable
internal fun ChannelAboutSection(channel: ChannelFeed) {
    val uriHandler = LocalUriHandler.current
    val moreInfo =
        listOfNotNull(
            channel.canonicalUrl?.let { it.removePrefix("http://").removePrefix("https://") },
            channel.handle?.takeIf { it.isNotBlank() },
            channel.subscribers?.takeIf { it.isNotBlank() },
            channel.videoCount?.takeIf { it.isNotBlank() },
            channel.viewCount?.takeIf { it.isNotBlank() },
            channel.country?.takeIf { it.isNotBlank() },
            channel.joinedDate?.takeIf { it.isNotBlank() },
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(AboutPadding),
        verticalArrangement = Arrangement.spacedBy(AboutSectionSpacing),
    ) {
        channel.description?.takeIf { it.isNotBlank() }?.let { description ->
            Column(verticalArrangement = Arrangement.spacedBy(AboutBlockSpacing)) {
                AboutHeading(stringResource(Res.string.channel_tab_about))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (channel.links.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = TabDividerThickness)
            Column(verticalArrangement = Arrangement.spacedBy(AboutRowSpacing)) {
                AboutHeading(stringResource(Res.string.channel_about_links))
                channel.links.forEach { link ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { uriHandler.openUri(link.url) },
                    ) {
                        Text(
                            text = link.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = link.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (moreInfo.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = TabDividerThickness)
            Column(verticalArrangement = Arrangement.spacedBy(AboutRowSpacing)) {
                AboutHeading(stringResource(Res.string.channel_about_more_info))
                moreInfo.forEach { value ->
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AboutHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}
