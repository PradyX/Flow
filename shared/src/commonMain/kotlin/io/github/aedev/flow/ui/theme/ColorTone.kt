package io.github.aedev.flow.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.pow

private const val TONE_STEP = 4.0
private const val LIGHTEST_TONE = 98.0
private const val DARKEST_TONE = 2.0
private const val MID_TONE = 50.0

// D65 reference white, which is the white point CIELAB is defined against.
private const val WHITE_X = 95.047
private const val WHITE_Y = 100.0
private const val WHITE_Z = 108.883

private const val LAB_PIVOT = 0.008856
private const val LAB_PIVOT_CUBED = 0.206893034
private const val LAB_SLOPE = 7.787
private const val LAB_OFFSET = 16.0 / 116.0

private fun channelToLinear(channel: Float): Double {
    val value = channel.toDouble()
    return if (value > 0.04045) ((value + 0.055) / 1.055).pow(2.4) else value / 12.92
}

private fun linearToChannel(linear: Double): Float {
    val value = if (linear > 0.0031308) 1.055 * linear.pow(1.0 / 2.4) - 0.055 else 12.92 * linear
    return value.coerceIn(0.0, 1.0).toFloat()
}

/** CIELAB (L*, a*, b*) for this colour, the space the palette tones are reasoned in. */
private fun Color.toLab(): DoubleArray {
    val r = channelToLinear(red)
    val g = channelToLinear(green)
    val b = channelToLinear(blue)

    val x = (r * 0.4124 + g * 0.3576 + b * 0.1805) * 100.0
    val y = (r * 0.2126 + g * 0.7152 + b * 0.0722) * 100.0
    val z = (r * 0.0193 + g * 0.1192 + b * 0.9505) * 100.0

    fun pivot(t: Double): Double = if (t > LAB_PIVOT) cbrt(t) else LAB_SLOPE * t + LAB_OFFSET

    val fx = pivot(x / WHITE_X)
    val fy = pivot(y / WHITE_Y)
    val fz = pivot(z / WHITE_Z)

    return doubleArrayOf(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
}

private fun labToColor(
    lightness: Double,
    a: Double,
    b: Double,
): Color {
    val fy = (lightness + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0

    fun unpivot(t: Double): Double = if (t > LAB_PIVOT_CUBED) t * t * t else (t - LAB_OFFSET) / LAB_SLOPE

    val x = unpivot(fx) * WHITE_X / 100.0
    val y = unpivot(fy) * WHITE_Y / 100.0
    val z = unpivot(fz) * WHITE_Z / 100.0

    return Color(
        red = linearToChannel(x * 3.2406 + y * -1.5372 + z * -0.4986),
        green = linearToChannel(x * -0.9689 + y * 1.8758 + z * 0.0415),
        blue = linearToChannel(x * 0.0557 + y * -0.2040 + z * 1.0570),
        alpha = 1f,
    )
}

internal fun Color.tone(): Double = toLab()[0]

internal fun Color.withTone(tone: Double): Color {
    val lab = toLab()
    return labToColor(tone.coerceIn(0.0, 100.0), lab[1], lab[2])
}

/** [withTone], with the colourfulness held to at most [maxChroma] (CIELAB a*b* distance). */
internal fun Color.withTone(
    tone: Double,
    maxChroma: Double,
): Color {
    val lab = toLab()
    val chroma = hypot(lab[1], lab[2])
    val scale = if (chroma > maxChroma) maxChroma / chroma else 1.0
    return labToColor(tone.coerceIn(0.0, 100.0), lab[1] * scale, lab[2] * scale)
}

/** WCAG contrast ratio between two opaque colors. */
internal fun contrastRatio(
    a: Color,
    b: Color,
): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/** Moves [color] away from [surface] in tone until it clears [minRatio]. */
internal fun ensureContrastOn(
    color: Color,
    surface: Color,
    minRatio: Float,
): Color {
    val lighten = surface.tone() < MID_TONE
    var candidate = color
    var tone = candidate.tone()
    while (contrastRatio(candidate, surface) < minRatio && if (lighten) tone < LIGHTEST_TONE else tone > DARKEST_TONE) {
        tone += if (lighten) TONE_STEP else -TONE_STEP
        candidate = candidate.withTone(tone)
    }
    return candidate
}
