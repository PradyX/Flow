package io.github.aedev.flow.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Dimensions {
    val ListItemHeight: Dp = 64.dp
    val ListThumbnailSize: Dp = 48.dp
    val SuggestionItemHeight: Dp = 56.dp

    val GridThumbnailHeightBig: Dp = 128.dp
    val GridThumbnailHeightSmall: Dp = 104.dp
    val AlbumThumbnailSize: Dp = 144.dp

    val ThumbnailCornerRadius: Dp = 6.dp
    val CardCornerRadius: Dp = 8.dp

    val MoodButtonHeight: Dp = 48.dp

    /** The space a feed reserves above its first band, on top of the content inset. */
    val FeedTopPadding: Dp = 4.dp

    /** The feed's own side inset; a band inside the feed adds the content padding on top. */
    val FeedSideInset: Dp = 4.dp

    val ContentPaddingHorizontal: Dp = 12.dp
    val ContentPaddingVertical: Dp = 12.dp
    val ItemSpacing: Dp = 12.dp
    val SectionSpacing: Dp = 16.dp

    /** The gap between the dense tiles of a music grid, in both directions. */
    val DenseTileGap: Dp = 8.dp

    /** The portrait an artist card carries. */
    val ArtistPortraitSize: Dp = 84.dp

    val PlayerHorizontalPadding: Dp = 32.dp
}

enum class GridItemSize {
    BIG,
    SMALL,
    ;

    val thumbnailHeight: Dp
        get() =
            when (this) {
                BIG -> Dimensions.GridThumbnailHeightBig
                SMALL -> Dimensions.GridThumbnailHeightSmall
            }
}
