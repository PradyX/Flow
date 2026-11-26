@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.aedev.flow.ui.shapes

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.graphics.shapes.Morph

/**
 * Draws [morph] at [progress], [extent] across, centred on [center] and turned by [degrees]. [path]
 * is reused between frames so a morph drawn from a clock allocates nothing while it runs.
 */
fun DrawScope.drawMorph(
    morph: Morph,
    progress: Float,
    path: Path,
    center: Offset,
    extent: Float,
    degrees: Float,
    color: Color,
) {
    morph.toPath(progress, path)
    withTransform({
        translate(center.x - extent / 2f, center.y - extent / 2f)
        rotate(degrees, pivot = Offset(extent / 2f, extent / 2f))
        scale(extent, extent, pivot = Offset.Zero)
    }) { drawPath(path, color) }
}
