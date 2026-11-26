package io.github.aedev.flow.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.data.search.SearchHistory
import io.github.aedev.flow.presentation.ui.FlowIcons
import io.github.aedev.flow.presentation.ui.FlowNavRow
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.search_recent_clear
import io.github.aedev.flow.resources.search_recent_remove
import io.github.aedev.flow.resources.search_recent_title
import io.github.aedev.flow.ui.theme.Dimensions
import org.jetbrains.compose.resources.stringResource

/** YouTube's completions for the term being typed, under the field, in the server's own order. */
@Composable
internal fun SearchSuggestionRows(
    suggestions: List<String>,
    onPick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(suggestions, key = { it }) { suggestion ->
            FlowNavRow(
                title = suggestion,
                onClick = { onPick(suggestion) },
                leadingIcon = FlowIcons.Search,
                showChevron = false,
            )
        }
    }
}

/**
 * The searches this device ran, under the field while it is empty: newest first, each removable,
 * with the whole list cleared from its heading.
 */
@Composable
internal fun SearchHistoryRows(
    history: SearchHistory,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (history.isEmpty) return
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimensions.ContentPaddingHorizontal,
                        end = 4.dp,
                        top = 12.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.search_recent_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) {
                Text(stringResource(Res.string.search_recent_clear))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(history.queries, key = { it }) { term ->
                FlowNavRow(
                    title = term,
                    onClick = { onPick(term) },
                    leadingIcon = FlowIcons.History,
                    showChevron = false,
                    trailingContent = {
                        IconButton(onClick = { onRemove(term) }) {
                            Icon(
                                imageVector = FlowIcons.Close,
                                contentDescription = stringResource(Res.string.search_recent_remove),
                            )
                        }
                    },
                )
            }
        }
    }
}
