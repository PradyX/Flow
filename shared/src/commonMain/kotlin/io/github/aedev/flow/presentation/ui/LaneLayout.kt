package io.github.aedev.flow.presentation.ui

import androidx.compose.ui.unit.Dp

/**
 * The width one cell of a [columns]-wide grid takes across [contentWidth], once [gap] separates
 * the columns. Fills the width exactly, so a row of cells always ends on the content edge.
 */
internal fun gridCellWidth(
    contentWidth: Dp,
    columns: Int,
    gap: Dp,
): Dp = (contentWidth - gap * (columns - 1)) / columns
