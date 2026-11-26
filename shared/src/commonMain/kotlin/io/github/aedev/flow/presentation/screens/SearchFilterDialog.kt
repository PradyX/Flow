package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.presentation.feed.SearchDuration
import io.github.aedev.flow.presentation.feed.SearchFeature
import io.github.aedev.flow.presentation.feed.SearchFilter
import io.github.aedev.flow.presentation.feed.SearchSort
import io.github.aedev.flow.presentation.feed.SearchType
import io.github.aedev.flow.presentation.feed.SearchUploadDate
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.action_apply
import io.github.aedev.flow.resources.action_cancel
import io.github.aedev.flow.resources.search_date_any
import io.github.aedev.flow.resources.search_date_last_hour
import io.github.aedev.flow.resources.search_date_this_month
import io.github.aedev.flow.resources.search_date_this_week
import io.github.aedev.flow.resources.search_date_this_year
import io.github.aedev.flow.resources.search_date_today
import io.github.aedev.flow.resources.search_duration_3_to_20
import io.github.aedev.flow.resources.search_duration_any
import io.github.aedev.flow.resources.search_duration_over_20
import io.github.aedev.flow.resources.search_duration_under_3
import io.github.aedev.flow.resources.search_feature_360
import io.github.aedev.flow.resources.search_feature_3d
import io.github.aedev.flow.resources.search_feature_4k
import io.github.aedev.flow.resources.search_feature_creative_commons
import io.github.aedev.flow.resources.search_feature_hd
import io.github.aedev.flow.resources.search_feature_hdr
import io.github.aedev.flow.resources.search_feature_live
import io.github.aedev.flow.resources.search_feature_location
import io.github.aedev.flow.resources.search_feature_purchased
import io.github.aedev.flow.resources.search_feature_subtitles
import io.github.aedev.flow.resources.search_feature_vr180
import io.github.aedev.flow.resources.search_filter_group_duration
import io.github.aedev.flow.resources.search_filter_group_features
import io.github.aedev.flow.resources.search_filter_group_sort
import io.github.aedev.flow.resources.search_filter_group_type
import io.github.aedev.flow.resources.search_filter_group_upload_date
import io.github.aedev.flow.resources.search_filters_title
import io.github.aedev.flow.resources.search_sort_relevance
import io.github.aedev.flow.resources.search_sort_view_count
import io.github.aedev.flow.resources.search_type_any
import io.github.aedev.flow.resources.search_type_channel
import io.github.aedev.flow.resources.search_type_movie
import io.github.aedev.flow.resources.search_type_playlist
import io.github.aedev.flow.resources.search_type_shorts
import io.github.aedev.flow.resources.search_type_video
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Search filters, laid out the way the app's own dialog is: one row per single-choice group with
 * the current value on the right, then the multi-choice features as chips, then Cancel and Apply.
 *
 * Choices are held locally until Apply, so a half-made selection never restarts the page.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SearchFilterDialog(
    filter: SearchFilter,
    onApply: (SearchFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(filter) { mutableStateOf(filter) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.search_filters_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FilterRowSpacing),
            ) {
                FilterRow(
                    label = stringResource(Res.string.search_filter_group_type),
                    value = stringResource(draft.type.label()),
                    options = SearchType.entries,
                    optionLabel = { stringResource(it.label()) },
                    onSelect = { draft = draft.copy(type = it) },
                )

                if (draft.videoFiltersVisible) {
                    FilterRow(
                        label = stringResource(Res.string.search_filter_group_duration),
                        value = stringResource(draft.duration.label()),
                        options = SearchDuration.entries,
                        optionLabel = { stringResource(it.label()) },
                        onSelect = { draft = draft.copy(duration = it) },
                    )
                    FilterRow(
                        label = stringResource(Res.string.search_filter_group_upload_date),
                        value = stringResource(draft.uploadDate.label()),
                        options = SearchUploadDate.entries,
                        optionLabel = { stringResource(it.label()) },
                        onSelect = { draft = draft.copy(uploadDate = it) },
                    )
                }

                FilterRow(
                    label = stringResource(Res.string.search_filter_group_sort),
                    value = stringResource(draft.sort.label()),
                    options = SearchSort.entries,
                    optionLabel = { stringResource(it.label()) },
                    onSelect = { draft = draft.copy(sort = it) },
                )

                if (draft.videoFiltersVisible) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(Res.string.search_filter_group_features),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FeatureChipSpacing),
                        verticalArrangement = Arrangement.spacedBy(FeatureChipSpacing),
                    ) {
                        SearchFeature.entries.forEach { feature ->
                            val selected = feature in draft.features
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    draft =
                                        draft.copy(
                                            features =
                                                if (selected) {
                                                    draft.features - feature
                                                } else {
                                                    draft.features + feature
                                                },
                                        )
                                },
                                label = { Text(stringResource(feature.label())) },
                                shapes = FilterChipDefaults.shapes(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) {
                Text(stringResource(Res.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

/** One single-choice group: the label and the current value, with the choices in a menu. */
@Composable
private fun <T> FilterRow(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(vertical = FilterRowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = FlowIcons.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DropdownIconSize),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val text = optionLabel(option)
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

private fun SearchType.label(): StringResource =
    when (this) {
        SearchType.ANY -> Res.string.search_type_any
        SearchType.VIDEO -> Res.string.search_type_video
        SearchType.CHANNEL -> Res.string.search_type_channel
        SearchType.PLAYLIST -> Res.string.search_type_playlist
        SearchType.MOVIE -> Res.string.search_type_movie
        SearchType.SHORTS -> Res.string.search_type_shorts
    }

private fun SearchDuration.label(): StringResource =
    when (this) {
        SearchDuration.ANY -> Res.string.search_duration_any
        SearchDuration.UNDER_3_MINUTES -> Res.string.search_duration_under_3
        SearchDuration.THREE_TO_20_MINUTES -> Res.string.search_duration_3_to_20
        SearchDuration.OVER_20_MINUTES -> Res.string.search_duration_over_20
    }

private fun SearchUploadDate.label(): StringResource =
    when (this) {
        SearchUploadDate.ANY -> Res.string.search_date_any
        SearchUploadDate.LAST_HOUR -> Res.string.search_date_last_hour
        SearchUploadDate.TODAY -> Res.string.search_date_today
        SearchUploadDate.THIS_WEEK -> Res.string.search_date_this_week
        SearchUploadDate.THIS_MONTH -> Res.string.search_date_this_month
        SearchUploadDate.THIS_YEAR -> Res.string.search_date_this_year
    }

private fun SearchSort.label(): StringResource =
    when (this) {
        SearchSort.RELEVANCE -> Res.string.search_sort_relevance
        SearchSort.VIEW_COUNT -> Res.string.search_sort_view_count
    }

private fun SearchFeature.label(): StringResource =
    when (this) {
        SearchFeature.HD -> Res.string.search_feature_hd
        SearchFeature.FOUR_K -> Res.string.search_feature_4k
        SearchFeature.HDR -> Res.string.search_feature_hdr
        SearchFeature.SUBTITLES -> Res.string.search_feature_subtitles
        SearchFeature.CREATIVE_COMMONS -> Res.string.search_feature_creative_commons
        SearchFeature.THREE_SIXTY -> Res.string.search_feature_360
        SearchFeature.VR180 -> Res.string.search_feature_vr180
        SearchFeature.THREE_D -> Res.string.search_feature_3d
        SearchFeature.LOCATION -> Res.string.search_feature_location
        SearchFeature.PURCHASED -> Res.string.search_feature_purchased
        SearchFeature.LIVE -> Res.string.search_feature_live
    }

private val FilterRowSpacing = 4.dp
private val FilterRowVerticalPadding = 10.dp
private val FeatureChipSpacing = 8.dp
private val DropdownIconSize = 24.dp
