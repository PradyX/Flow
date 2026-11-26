@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

private val GroupedRowHorizontalPadding = 16.dp
private val GroupHorizontalPadding = 12.dp
private val NavRowVerticalPadding = 14.dp
private val SwitchRowVerticalPadding = 6.dp
private val RowLeadingIconSize = 22.dp
private val NavRowTrailingIconSize = 20.dp

private const val DISABLED_CONTENT_ALPHA = 0.4f

/** Vertical gap between the rows of a segmented group, from the list-item tokens. */
val FlowSegmentedGap = ListItemDefaults.SegmentedGap

/** The shape of row [index] of [count] in a segmented group: the outer ends round, the rest small. */
@Composable
fun flowRowGroupShape(
    index: Int,
    count: Int,
): Shape = ListItemDefaults.segmentedShapes(index = index, count = count).shape

/** Lays rows out as one segmented group, with the token gap between them. */
@Composable
fun FlowRowGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = GroupHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(FlowSegmentedGap),
        content = content,
    )
}

/**
 * A row that opens something or runs an action: leading glyph, title and supporting line, then an
 * optional trailing slot or a chevron. [shape] is the row's slice of its segmented group.
 */
@Composable
fun FlowNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    stateDescription: String? = null,
) {
    val trailing: (@Composable () -> Unit)? =
        trailingContent ?: if (showChevron) {
            {
                Icon(
                    imageVector = FlowIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(NavRowTrailingIconSize),
                )
            }
        } else {
            null
        }

    SegmentedListItem(
        verticalAlignment = Alignment.CenterVertically,
        onClick = onClick,
        shapes = ListItemDefaults.shapes(shape = shape),
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (stateDescription != null) Modifier.semantics { this.stateDescription = stateDescription } else Modifier),
        enabled = enabled,
        supportingContent = supportingText?.let { text -> { Text(text = text) } },
        leadingContent =
            leadingContent ?: leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(RowLeadingIconSize),
                    )
                }
            },
        trailingContent = trailing,
        colors = flowRowColors(shape),
        contentPadding = rowPadding(shape, NavRowVerticalPadding),
    ) {
        Text(text = title)
    }
}

/** A row whose whole width toggles a trailing switch. */
@Composable
fun FlowSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
) {
    SegmentedListItem(
        verticalAlignment = Alignment.CenterVertically,
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.shapes(shape = shape),
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Switch
                    toggleableState = ToggleableState(checked)
                },
        enabled = enabled,
        supportingContent = supportingText?.let { text -> { Text(text = text) } },
        leadingContent =
            leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(RowLeadingIconSize),
                    )
                }
            },
        trailingContent = {
            Switch(checked = checked, enabled = enabled, onCheckedChange = null)
        },
        colors = flowRowColors(shape),
        contentPadding = rowPadding(shape, SwitchRowVerticalPadding),
    ) {
        Text(text = title)
    }
}

/** Colours for a row: a grouped row sits on its own container, an ungrouped row stays clear. */
@Composable
private fun flowRowColors(shape: Shape): ListItemColors {
    val container = if (shape == RectangleShape) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh
    val colors = MaterialTheme.colorScheme
    return ListItemDefaults.segmentedColors(
        containerColor = container,
        contentColor = colors.onSurface,
        leadingContentColor = colors.onSurfaceVariant,
        trailingContentColor = colors.onSurfaceVariant,
        supportingContentColor = colors.onSurfaceVariant,
        disabledContainerColor = container,
        disabledContentColor = colors.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
        disabledLeadingContentColor = colors.onSurfaceVariant.copy(alpha = DISABLED_CONTENT_ALPHA),
        disabledTrailingContentColor = colors.onSurfaceVariant.copy(alpha = DISABLED_CONTENT_ALPHA),
        disabledSupportingContentColor = colors.onSurfaceVariant.copy(alpha = DISABLED_CONTENT_ALPHA),
        selectedContainerColor = colors.secondaryContainer,
        selectedContentColor = colors.onSecondaryContainer,
        selectedLeadingContentColor = colors.onSecondaryContainer,
        selectedTrailingContentColor = colors.primary,
        selectedSupportingContentColor = colors.onSecondaryContainer,
    )
}

private fun rowPadding(
    shape: Shape,
    vertical: androidx.compose.ui.unit.Dp,
): PaddingValues =
    PaddingValues(
        horizontal = if (shape == RectangleShape) 20.dp else GroupedRowHorizontalPadding,
        vertical = vertical,
    )
