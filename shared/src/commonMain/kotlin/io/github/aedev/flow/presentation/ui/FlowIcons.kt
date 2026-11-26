package io.github.aedev.flow.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The glyphs the shared chrome draws: home, search, back and play.
 *
 * They are built here instead of coming from a Material icons artifact, so the shared module keeps
 * its dependency set and every target renders the same shapes. Each vector is a black fill; `Icon`
 * tints it with the surrounding content colour.
 */
internal object FlowIcons {
    // The navigation bar's icons are the reference app's own set: Material's filled and outlined
    // drawings for every tab, plus the app's custom Shorts mark, so a tab reads the same in both.

    val HomeFilled: ImageVector by lazy {
        pathIcon("HomeFilled", "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z")
    }

    val HomeOutlined: ImageVector by lazy {
        pathIcon("HomeOutlined", "M12 5.69l5 4.5V18h-2v-6H9v6H7v-7.81l5-4.5M12 3L2 12h3v8h6v-6h2v6h6v-8h3L12 3z")
    }

    val MusicNoteFilled: ImageVector by lazy {
        pathIcon(
            "MusicNoteFilled",
            "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z",
        )
    }

    val MusicNoteOutlined: ImageVector by lazy {
        pathIcon(
            "MusicNoteOutlined",
            "M12 3l.01 10.55c-.59-.34-1.27-.55-2-.55C7.79 13 6 14.79 6 17s1.79 4 4.01 4S14 19.21 14 17V7h4V3h-6zm-1.99 16c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z",
        )
    }

    val SubscriptionsFilled: ImageVector by lazy {
        pathIcon(
            "SubscriptionsFilled",
            "M20 8H4V6h16v2zm-2-6H6v2h12V2zm4 10v8c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2v-8c0-1.1.9-2 2-2h16c1.1 0 2 .9 2 2zm-6 4l-6-3.27v6.53L16 16z",
        )
    }

    val SubscriptionsOutlined: ImageVector by lazy {
        pathIcon(
            "SubscriptionsOutlined",
            "M4 6h16v2H4zm2-4h12v2H6zm14 8H4c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-8c0-1.1-.9-2-2-2zm0 10H4v-8h16v8zm-10-7.27v6.53L16 16z",
        )
    }

    val VideoLibraryFilled: ImageVector by lazy {
        pathIcon(
            "VideoLibraryFilled",
            "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8 12.5v-9l6 4.5-6 4.5z",
        )
    }

    val VideoLibraryOutlined: ImageVector by lazy {
        pathIcon(
            "VideoLibraryOutlined",
            "M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12zM12 5.5v9l6-4.5z",
        )
    }

    val ExploreFilled: ImageVector by lazy {
        pathIcon(
            "ExploreFilled",
            "M12 10.9c-.61 0-1.1.49-1.1 1.1s.49 1.1 1.1 1.1c.61 0 1.1-.49 1.1-1.1s-.49-1.1-1.1-1.1zM12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm2.19 12.19L6 18l3.81-8.19L18 6l-3.81 8.19z",
        )
    }

    val ExploreOutlined: ImageVector by lazy {
        pathIcon(
            "ExploreOutlined",
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-5.5-2.5l7.51-3.49L17.5 6.5 9.99 9.99 6.5 17.5zm5.5-6.6c.61 0 1.1.49 1.1 1.1s-.49 1.1-1.1 1.1-1.1-.49-1.1-1.1.49-1.1 1.1-1.1z",
        )
    }

    val SearchFilled: ImageVector by lazy {
        pathIcon(
            "SearchFilled",
            "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
        )
    }

    val SearchOutlined: ImageVector get() = SearchFilled

    /** The reference app's Shorts mark: a stroked double-arrow ribbon, filled when selected. */
    val ShortsOutlined: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ShortsOutlined",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 144f,
                viewportHeight = 144f,
            ).apply {
                group(translationX = 8f, translationY = 8f) {
                    addPath(
                        pathData =
                            addPathNodes(
                                "M63.49 2.71c11.59-6.04 25.94-1.64 32.04 9.83 6.1 11.47 1.65 25.66-9.94 31.7l-9.53 5.01c8.21.3 16.04 4.81 20.14 12.52 6.1 11.47 1.66 25.66-9.94 31.7l-50.82 26.7c-11.59 6.04-25.94 1.64-32.04-9.83-6.1-11.47-1.65-25.66 9.94-31.7l9.53-5.01c-8.21-.3-16.04-4.81-20.14-12.52-6.1-11.47-1.65-25.66 9.94-31.7l50.82-26.7zM36.06 42.53l30.76 18.99-30.76 18.9V42.53z",
                            ),
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 8f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                        pathFillType = PathFillType.EvenOdd,
                    )
                }
            }.build()
    }

    val ShortsFilled: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ShortsFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 144f,
                viewportHeight = 144f,
            ).apply {
                group(
                    translationX = 8f,
                    translationY = 8f,
                    pivotX = 64f,
                    pivotY = 64f,
                    scaleX = 1.03f,
                    scaleY = 1.03f,
                ) {
                    addPath(
                        pathData =
                            addPathNodes(
                                "M63.49 2.71c11.59-6.04 25.94-1.64 32.04 9.83 6.1 11.47 1.65 25.66-9.94 31.7l-9.53 5.01c8.21.3 16.04 4.81 20.14 12.52 6.1 11.47 1.66 25.66-9.94 31.7l-50.82 26.7c-11.59 6.04-25.94 1.64-32.04-9.83-6.1-11.47-1.65-25.66 9.94-31.7l9.53-5.01c-8.21-.3-16.04-4.81-20.14-12.52-6.1-11.47-1.65-25.66 9.94-31.7l50.82-26.7zM36.06 42.53l30.76 18.99-30.76 18.9V42.53z",
                            ),
                        fill = SolidColor(Color.Black),
                        pathFillType = PathFillType.EvenOdd,
                    )
                }
            }.build()
    }

    val Home: ImageVector by lazy {
        icon("Home") {
            moveTo(12f, 3f)
            lineTo(2f, 12f)
            lineTo(4f, 14f)
            lineTo(5f, 13f)
            lineTo(5f, 21f)
            lineTo(10.5f, 21f)
            lineTo(10.5f, 15f)
            lineTo(13.5f, 15f)
            lineTo(13.5f, 21f)
            lineTo(19f, 21f)
            lineTo(19f, 13f)
            lineTo(20f, 14f)
            lineTo(22f, 12f)
            close()
        }
    }

    val Search: ImageVector by lazy {
        icon("Search") {
            circle(cx = 10.5f, cy = 10.5f, r = 7f)
            circle(cx = 10.5f, cy = 10.5f, r = 5f)
            moveTo(16.1f, 14.7f)
            lineTo(21.9f, 20.5f)
            lineTo(20.5f, 21.9f)
            lineTo(14.7f, 16.1f)
            close()
        }
    }

    /** A compass: the Explore tab's mark, as the app's own navigation uses. */
    val Explore: ImageVector by lazy {
        icon("Explore") {
            circle(cx = 12f, cy = 12f, r = 10f)
            circle(cx = 12f, cy = 12f, r = 8.4f)
            moveTo(15.6f, 8.4f)
            lineTo(11f, 11f)
            lineTo(8.4f, 15.6f)
            lineTo(13f, 13f)
            close()
        }
    }

    /** A four-tile grid: the music tab's moods and genres entry. */
    val Genres: ImageVector by lazy {
        icon("Genres") {
            moveTo(3f, 3f)
            lineTo(10.5f, 3f)
            lineTo(10.5f, 10.5f)
            lineTo(3f, 10.5f)
            close()
            moveTo(13.5f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 10.5f)
            lineTo(13.5f, 10.5f)
            close()
            moveTo(3f, 13.5f)
            lineTo(10.5f, 13.5f)
            lineTo(10.5f, 21f)
            lineTo(3f, 21f)
            close()
            moveTo(13.5f, 13.5f)
            lineTo(21f, 13.5f)
            lineTo(21f, 21f)
            lineTo(13.5f, 21f)
            close()
        }
    }

    val Back: ImageVector by lazy {
        icon("Back") {
            moveTo(20f, 11f)
            lineTo(8.8f, 11f)
            lineTo(14.4f, 5.4f)
            lineTo(13f, 4f)
            lineTo(5f, 12f)
            lineTo(13f, 20f)
            lineTo(14.4f, 18.6f)
            lineTo(8.8f, 13f)
            lineTo(20f, 13f)
            close()
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            moveTo(6.4f, 5f)
            lineTo(12f, 10.6f)
            lineTo(17.6f, 5f)
            lineTo(19f, 6.4f)
            lineTo(13.4f, 12f)
            lineTo(19f, 17.6f)
            lineTo(17.6f, 19f)
            lineTo(12f, 13.4f)
            lineTo(6.4f, 19f)
            lineTo(5f, 17.6f)
            lineTo(10.6f, 12f)
            lineTo(5f, 6.4f)
            close()
        }
    }

    val Play: ImageVector by lazy {
        icon("Play") {
            moveTo(8f, 5f)
            lineTo(19f, 12f)
            lineTo(8f, 19f)
            close()
        }
    }

    /** The filled circle with a play cut out of it, as the Continue Watching header draws. */
    val PlayCircle: ImageVector by lazy {
        pathIcon("PlayCircle", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z")
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            moveTo(6f, 5f)
            lineTo(10f, 5f)
            lineTo(10f, 19f)
            lineTo(6f, 19f)
            close()
            moveTo(14f, 5f)
            lineTo(18f, 5f)
            lineTo(18f, 19f)
            lineTo(14f, 19f)
            close()
        }
    }

    /** Two arrows crossing: the reference's Shuffle button. */
    val Shuffle: ImageVector by lazy {
        icon("Shuffle") {
            moveTo(10.59f, 9.17f)
            lineTo(5.41f, 4f)
            lineTo(4f, 5.41f)
            lineTo(9.17f, 10.59f)
            close()
            moveTo(14.5f, 4f)
            lineTo(16.54f, 6.04f)
            lineTo(4f, 18.59f)
            lineTo(5.41f, 20f)
            lineTo(17.96f, 7.46f)
            lineTo(20f, 9.5f)
            lineTo(20f, 4f)
            close()
            moveTo(14.83f, 13.41f)
            lineTo(13.42f, 14.82f)
            lineTo(16.55f, 17.95f)
            lineTo(14.5f, 20f)
            lineTo(20f, 20f)
            lineTo(20f, 14.5f)
            lineTo(17.96f, 16.54f)
            close()
        }
    }

    /** A play triangle with an arrow leaving it: the reference's "Play next". */
    val QueuePlayNext: ImageVector by lazy {
        icon("QueuePlayNext") {
            moveTo(3f, 6f)
            lineTo(13f, 6f)
            lineTo(13f, 8f)
            lineTo(3f, 8f)
            close()
            moveTo(3f, 11f)
            lineTo(11f, 11f)
            lineTo(11f, 13f)
            lineTo(3f, 13f)
            close()
            moveTo(3f, 16f)
            lineTo(11f, 16f)
            lineTo(11f, 18f)
            lineTo(3f, 18f)
            close()
            moveTo(15f, 9f)
            lineTo(21f, 13f)
            lineTo(15f, 17f)
            close()
        }
    }

    /** A play triangle dropping into a tray: the reference's "Add to queue". */
    val AddToQueue: ImageVector by lazy {
        icon("AddToQueue") {
            moveTo(3f, 6f)
            lineTo(13f, 6f)
            lineTo(13f, 8f)
            lineTo(3f, 8f)
            close()
            moveTo(3f, 11f)
            lineTo(11f, 11f)
            lineTo(11f, 13f)
            lineTo(3f, 13f)
            close()
            moveTo(3f, 16f)
            lineTo(11f, 16f)
            lineTo(11f, 18f)
            lineTo(3f, 18f)
            close()
            moveTo(17f, 8f)
            lineTo(19f, 8f)
            lineTo(19f, 12f)
            lineTo(21f, 12f)
            lineTo(21f, 14f)
            lineTo(19f, 14f)
            lineTo(19f, 18f)
            lineTo(17f, 18f)
            lineTo(17f, 14f)
            lineTo(15f, 14f)
            lineTo(15f, 12f)
            lineTo(17f, 12f)
            close()
        }
    }

    /** Ticked boxes: the reference's select-mode control. */
    val Checklist: ImageVector by lazy {
        icon("Checklist") {
            moveTo(3f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 7f)
            lineTo(3f, 7f)
            close()
            moveTo(3f, 11f)
            lineTo(13f, 11f)
            lineTo(13f, 13f)
            lineTo(3f, 13f)
            close()
            moveTo(3f, 17f)
            lineTo(13f, 17f)
            lineTo(13f, 19f)
            lineTo(3f, 19f)
            close()
            moveTo(17f, 4f)
            lineTo(22f, 9f)
            lineTo(17f, 14f)
            lineTo(15.6f, 12.6f)
            lineTo(19.2f, 9f)
            lineTo(15.6f, 5.4f)
            close()
        }
    }

    val SkipPrevious: ImageVector by lazy {
        icon("SkipPrevious") {
            moveTo(6f, 6f)
            lineTo(8f, 6f)
            lineTo(8f, 18f)
            lineTo(6f, 18f)
            close()
            moveTo(9.5f, 12f)
            lineTo(18f, 18f)
            lineTo(18f, 6f)
            close()
        }
    }

    /** Two arrows chasing each other: repeat the queue. */
    val Repeat: ImageVector by lazy {
        icon("Repeat") {
            moveTo(7f, 7f)
            lineTo(17f, 7f)
            lineTo(17f, 10f)
            lineTo(21f, 6f)
            lineTo(17f, 2f)
            lineTo(17f, 5f)
            lineTo(5f, 5f)
            lineTo(5f, 11f)
            lineTo(7f, 11f)
            close()
            moveTo(17f, 17f)
            lineTo(7f, 17f)
            lineTo(7f, 14f)
            lineTo(3f, 18f)
            lineTo(7f, 22f)
            lineTo(7f, 19f)
            lineTo(19f, 19f)
            lineTo(19f, 13f)
            lineTo(17f, 13f)
            close()
        }
    }

    /** The same arrows with a 1 in the middle: repeat the one track. */
    val RepeatOne: ImageVector by lazy {
        icon("RepeatOne") {
            moveTo(7f, 7f)
            lineTo(17f, 7f)
            lineTo(17f, 10f)
            lineTo(21f, 6f)
            lineTo(17f, 2f)
            lineTo(17f, 5f)
            lineTo(5f, 5f)
            lineTo(5f, 11f)
            lineTo(7f, 11f)
            close()
            moveTo(17f, 17f)
            lineTo(7f, 17f)
            lineTo(7f, 14f)
            lineTo(3f, 18f)
            lineTo(7f, 22f)
            lineTo(7f, 19f)
            lineTo(19f, 19f)
            lineTo(19f, 13f)
            lineTo(17f, 13f)
            close()
            moveTo(13f, 15f)
            lineTo(13f, 9f)
            lineTo(12f, 9f)
            lineTo(10f, 10f)
            lineTo(10f, 11f)
            lineTo(11.5f, 11f)
            lineTo(11.5f, 15f)
            close()
        }
    }

    val SkipNext: ImageVector by lazy {
        icon("SkipNext") {
            moveTo(6f, 18f)
            lineTo(14.5f, 12f)
            lineTo(6f, 6f)
            close()
            moveTo(16f, 6f)
            lineTo(18f, 6f)
            lineTo(18f, 18f)
            lineTo(16f, 18f)
            close()
        }
    }

    /** The bookmark's outline, for a page that is not saved yet. */
    val BookmarkBorder: ImageVector by lazy {
        icon("BookmarkBorder") {
            moveTo(5f, 3f)
            lineTo(19f, 3f)
            lineTo(19f, 21f)
            lineTo(12f, 18f)
            lineTo(5f, 21f)
            close()
            moveTo(7f, 5f)
            lineTo(7f, 18f)
            lineTo(12f, 15.8f)
            lineTo(17f, 18f)
            lineTo(17f, 5f)
            close()
        }
    }

    val ArrowForward: ImageVector by lazy {
        icon("ArrowForward") {
            moveTo(4f, 11f)
            lineTo(15.2f, 11f)
            lineTo(9.6f, 5.4f)
            lineTo(11f, 4f)
            lineTo(19f, 12f)
            lineTo(11f, 20f)
            lineTo(9.6f, 18.6f)
            lineTo(15.2f, 13f)
            lineTo(4f, 13f)
            close()
        }
    }

    /** A musical note: the stem, the flag and one round head. */
    val MusicNote: ImageVector by lazy {
        icon("MusicNote") {
            moveTo(11f, 3f)
            lineTo(13.4f, 3f)
            lineTo(13.4f, 15.4f)
            lineTo(11f, 15.4f)
            close()
            moveTo(13.4f, 3f)
            lineTo(20f, 4.6f)
            lineTo(20f, 7.4f)
            lineTo(13.4f, 5.8f)
            close()
            circle(cx = 8.6f, cy = 17f, r = 4.2f)
        }
    }

    /** A screen with a play mark: the app's video satellite. */
    val VideoFrame: ImageVector by lazy {
        icon("VideoFrame") {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            close()
            moveTo(5f, 7f)
            lineTo(5f, 17f)
            lineTo(19f, 17f)
            lineTo(19f, 7f)
            close()
            moveTo(10f, 9f)
            lineTo(16f, 12f)
            lineTo(10f, 15f)
            close()
        }
    }

    /** A circle with a play mark inside it. */
    val MotionVideo: ImageVector by lazy {
        icon("MotionVideo") {
            circle(cx = 12f, cy = 12f, r = 9f)
            moveTo(10f, 8f)
            lineTo(16.5f, 12f)
            lineTo(10f, 16f)
            close()
        }
    }

    /** The four-point star that heads an interest group. */
    val Sparkle: ImageVector by lazy {
        icon("Sparkle") {
            moveTo(12f, 2f)
            curveTo(13.2f, 7.4f, 16.6f, 10.8f, 22f, 12f)
            curveTo(16.6f, 13.2f, 13.2f, 16.6f, 12f, 22f)
            curveTo(10.8f, 16.6f, 7.4f, 13.2f, 2f, 12f)
            curveTo(7.4f, 10.8f, 10.8f, 7.4f, 12f, 2f)
            close()
        }
    }

    /** A sparkle with a smaller one beside it: the interests mark. */
    val Interests: ImageVector by lazy {
        icon("Interests") {
            moveTo(10f, 2f)
            curveTo(11f, 6.4f, 13.6f, 9f, 18f, 10f)
            curveTo(13.6f, 11f, 11f, 13.6f, 10f, 18f)
            curveTo(9f, 13.6f, 6.4f, 11f, 2f, 10f)
            curveTo(6.4f, 9f, 9f, 6.4f, 10f, 2f)
            close()
            moveTo(17.5f, 13f)
            curveTo(18.1f, 15.6f, 19.4f, 16.9f, 22f, 17.5f)
            curveTo(19.4f, 18.1f, 18.1f, 19.4f, 17.5f, 22f)
            curveTo(16.9f, 19.4f, 15.6f, 18.1f, 13f, 17.5f)
            curveTo(15.6f, 16.9f, 16.9f, 15.6f, 17.5f, 13f)
            close()
        }
    }

    /** A bell: the per-channel notification control. */
    val Bell: ImageVector by lazy {
        icon("Bell") {
            moveTo(12f, 2.6f)
            curveTo(8.4f, 2.6f, 6f, 5.2f, 6f, 8.8f)
            lineTo(6f, 14.4f)
            lineTo(3.6f, 17.4f)
            lineTo(3.6f, 19f)
            lineTo(20.4f, 19f)
            lineTo(20.4f, 17.4f)
            lineTo(18f, 14.4f)
            lineTo(18f, 8.8f)
            curveTo(18f, 5.2f, 15.6f, 2.6f, 12f, 2.6f)
            close()
            moveTo(9.4f, 20.4f)
            lineTo(14.6f, 20.4f)
            curveTo(14.2f, 21.6f, 13.2f, 22.2f, 12f, 22.2f)
            curveTo(10.8f, 22.2f, 9.8f, 21.6f, 9.4f, 20.4f)
            close()
        }
    }

    /** A play mark inside a card, under its title bar: the subscriptions mark. */
    val Subscriptions: ImageVector by lazy {
        icon("Subscriptions") {
            moveTo(4f, 3f)
            lineTo(20f, 3f)
            lineTo(20f, 5f)
            lineTo(4f, 5f)
            close()
            moveTo(3f, 8f)
            lineTo(21f, 8f)
            lineTo(21f, 21f)
            lineTo(3f, 21f)
            close()
            moveTo(5f, 10f)
            lineTo(5f, 19f)
            lineTo(19f, 19f)
            lineTo(19f, 10f)
            close()
            moveTo(10.5f, 11.8f)
            lineTo(16.2f, 14.5f)
            lineTo(10.5f, 17.2f)
            close()
        }
    }

    /** Points up, as the "New videos" pill's icon does. */
    val ArrowUpward: ImageVector by lazy {
        icon("ArrowUpward") {
            moveTo(4f, 12f)
            lineTo(5.41f, 13.41f)
            lineTo(11f, 7.83f)
            lineTo(11f, 20f)
            lineTo(13f, 20f)
            lineTo(13f, 7.83f)
            lineTo(18.58f, 13.42f)
            lineTo(20f, 12f)
            lineTo(12f, 4f)
            close()
        }
    }

    /** The menu arrow beside "Subscribed", matching the reference control's trailing segment. */
    val ArrowDropDown: ImageVector by lazy {
        icon("ArrowDropDown") {
            moveTo(7f, 10f)
            lineTo(12f, 15f)
            lineTo(17f, 10f)
            close()
        }
    }

    /** A person with a minus beside them: the unsubscribe item in the subscribed menu. */
    val PersonRemove: ImageVector by lazy {
        icon("PersonRemove") {
            circle(cx = 9f, cy = 7.5f, r = 3.5f)
            moveTo(2f, 20f)
            lineTo(2f, 17.8f)
            curveTo(2f, 14.6f, 6.4f, 13.6f, 9f, 13.6f)
            curveTo(11.6f, 13.6f, 16f, 14.6f, 16f, 17.8f)
            lineTo(16f, 20f)
            close()
            moveTo(16f, 8f)
            lineTo(22f, 8f)
            lineTo(22f, 10f)
            lineTo(16f, 10f)
            close()
        }
    }

    val Add: ImageVector by lazy {
        icon("Add") {
            moveTo(11f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 11f)
            lineTo(19f, 11f)
            lineTo(19f, 13f)
            lineTo(13f, 13f)
            lineTo(13f, 19f)
            lineTo(11f, 19f)
            lineTo(11f, 13f)
            lineTo(5f, 13f)
            lineTo(5f, 11f)
            lineTo(11f, 11f)
            close()
        }
    }

    /** An open tray: the imports' glyph. */
    val Inbox: ImageVector by lazy {
        icon("Inbox") {
            moveTo(3f, 5f)
            lineTo(21f, 5f)
            lineTo(21f, 19f)
            lineTo(3f, 19f)
            close()
            moveTo(5f, 7f)
            lineTo(5f, 13f)
            lineTo(9f, 13f)
            lineTo(10f, 15f)
            lineTo(14f, 15f)
            lineTo(15f, 13f)
            lineTo(19f, 13f)
            lineTo(19f, 7f)
            close()
        }
    }

    val Bookmark: ImageVector by lazy {
        icon("Bookmark") {
            moveTo(17f, 3f)
            curveTo(18.1f, 3f, 19f, 3.9f, 19f, 5f)
            lineTo(19f, 21f)
            lineTo(12f, 18f)
            lineTo(5f, 21f)
            lineTo(5f, 5f)
            curveTo(5f, 3.9f, 5.9f, 3f, 7f, 3f)
            close()
            moveTo(17f, 19f)
            lineTo(17f, 5f)
            lineTo(7f, 5f)
            lineTo(7f, 19f)
            lineTo(12f, 16.82f)
            close()
        }
    }

    val Download: ImageVector by lazy {
        icon("Download") {
            moveTo(19f, 9f)
            lineTo(15f, 9f)
            lineTo(15f, 3f)
            lineTo(9f, 3f)
            lineTo(9f, 9f)
            lineTo(5f, 9f)
            lineTo(12f, 16f)
            close()
            moveTo(5f, 18f)
            lineTo(19f, 18f)
            lineTo(19f, 20f)
            lineTo(5f, 20f)
            close()
        }
    }

    val ThumbUp: ImageVector by lazy {
        icon("ThumbUp") {
            thumbPath()
        }
    }

    /** The same thumb, turned over: one silhouette, so the pair reads as one control. */
    val ThumbDown: ImageVector by lazy {
        ImageVector
            .Builder(
                name = "ThumbDown",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                group(rotate = 180f, pivotX = 12f, pivotY = 12f) {
                    path(
                        fill = SolidColor(Color.Black),
                        pathFillType = PathFillType.EvenOdd,
                        pathBuilder = { thumbPath() },
                    )
                }
            }.build()
    }

    val SettingsOutlined: ImageVector by lazy {
        pathIcon(
            "SettingsOutlined",
            "M19.43 12.98c.04-.32.07-.64.07-.98 0-.34-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.09-.16-.26-.25-.44-.25-.06 0-.12.01-.17.03l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1c-.06-.02-.12-.03-.18-.03-.17 0-.34.09-.43.25l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65c-.04.32-.07.65-.07.98 0 .33.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.09.16.26.25.44.25.06 0 .12-.01.17-.03l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.06.02.12.03.18.03.17 0 .34-.09.43-.25l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zm-1.98-1.71c.04.31.05.52.05.73 0 .21-.02.43-.05.73l-.14 1.13.89.7 1.08.84-.7 1.21-1.27-.51-1.04-.42-.9.68c-.43.32-.84.56-1.25.73l-1.06.43-.16 1.13-.2 1.35h-1.4l-.19-1.35-.16-1.13-1.06-.43c-.43-.18-.83-.41-1.23-.71l-.91-.7-1.06.43-1.27.51-.7-1.21 1.08-.84.89-.7-.14-1.13c-.03-.31-.05-.54-.05-.74s.02-.43.05-.73l.14-1.13-.89-.7-1.08-.84.7-1.21 1.27.51 1.04.42.9-.68c.43-.32.84-.56 1.25-.73l1.06-.43.16-1.13.2-1.35h1.39l.19 1.35.16 1.13 1.06.43c.43.18.83.41 1.23.71l.91.7 1.06-.43 1.27-.51.7 1.21-1.07.85-.89.7.14 1.13zM12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm0 6c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z",
        )
    }

    val MoreHoriz: ImageVector by lazy {
        pathIcon(
            "MoreHoriz",
            "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
        )
    }

    val MoreVert: ImageVector by lazy {
        icon("MoreVert") {
            circle(cx = 12f, cy = 5f, r = 1.9f)
            circle(cx = 12f, cy = 12f, r = 1.9f)
            circle(cx = 12f, cy = 19f, r = 1.9f)
        }
    }

    /** A slash through a ring: the channel is not recommended any more. */
    val Block: ImageVector by lazy {
        icon("Block") {
            circle(cx = 12f, cy = 12f, r = 10f)
            circle(cx = 12f, cy = 12f, r = 7.6f)
            moveTo(4.6f, 4.6f)
            lineTo(19.4f, 19.4f)
            lineTo(17.9f, 20.9f)
            lineTo(3.1f, 6.1f)
            close()
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            moveTo(9.4f, 6f)
            lineTo(15.4f, 12f)
            lineTo(9.4f, 18f)
            lineTo(8f, 16.6f)
            lineTo(12.6f, 12f)
            lineTo(8f, 7.4f)
            close()
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            moveTo(9f, 16.2f)
            lineTo(4.8f, 12f)
            lineTo(3.4f, 13.4f)
            lineTo(9f, 19f)
            lineTo(21f, 7f)
            lineTo(19.6f, 5.6f)
            close()
        }
    }

    val Fullscreen: ImageVector by lazy {
        icon("Fullscreen") {
            moveTo(7f, 14f)
            lineTo(5f, 14f)
            lineTo(5f, 19f)
            lineTo(10f, 19f)
            lineTo(10f, 17f)
            lineTo(7f, 17f)
            close()
            moveTo(5f, 10f)
            lineTo(7f, 10f)
            lineTo(7f, 7f)
            lineTo(10f, 7f)
            lineTo(10f, 5f)
            lineTo(5f, 5f)
            close()
            moveTo(17f, 19f)
            lineTo(19f, 19f)
            lineTo(19f, 14f)
            lineTo(17f, 14f)
            lineTo(17f, 17f)
            lineTo(14f, 17f)
            lineTo(14f, 19f)
            close()
            moveTo(19f, 10f)
            lineTo(19f, 5f)
            lineTo(14f, 5f)
            lineTo(14f, 7f)
            lineTo(17f, 7f)
            lineTo(17f, 10f)
            close()
        }
    }

    /** The same corners turned inward: the control that leaves fullscreen. */
    val FullscreenExit: ImageVector by lazy {
        icon("FullscreenExit") {
            moveTo(5f, 16f)
            lineTo(8f, 16f)
            lineTo(8f, 19f)
            lineTo(10f, 19f)
            lineTo(10f, 14f)
            lineTo(5f, 14f)
            close()
            moveTo(8f, 8f)
            lineTo(5f, 8f)
            lineTo(5f, 10f)
            lineTo(10f, 10f)
            lineTo(10f, 5f)
            lineTo(8f, 5f)
            close()
            moveTo(14f, 19f)
            lineTo(16f, 19f)
            lineTo(16f, 16f)
            lineTo(19f, 16f)
            lineTo(19f, 14f)
            lineTo(14f, 14f)
            close()
            moveTo(16f, 8f)
            lineTo(16f, 5f)
            lineTo(14f, 5f)
            lineTo(14f, 10f)
            lineTo(19f, 10f)
            lineTo(19f, 8f)
            close()
        }
    }

    val Link: ImageVector by lazy {
        icon("Link") {
            moveTo(3.9f, 12f)
            curveTo(3.9f, 10.29f, 5.29f, 8.9f, 7f, 8.9f)
            lineTo(11f, 8.9f)
            lineTo(11f, 7f)
            lineTo(7f, 7f)
            curveTo(4.24f, 7f, 2f, 9.24f, 2f, 12f)
            curveTo(2f, 14.76f, 4.24f, 17f, 7f, 17f)
            lineTo(11f, 17f)
            lineTo(11f, 15.1f)
            lineTo(7f, 15.1f)
            curveTo(5.29f, 15.1f, 3.9f, 13.71f, 3.9f, 12f)
            close()
            moveTo(8f, 13f)
            lineTo(16f, 13f)
            lineTo(16f, 11f)
            lineTo(8f, 11f)
            close()
            moveTo(17f, 7f)
            lineTo(13f, 7f)
            lineTo(13f, 8.9f)
            lineTo(17f, 8.9f)
            curveTo(18.71f, 8.9f, 20.1f, 10.29f, 20.1f, 12f)
            curveTo(20.1f, 13.71f, 18.71f, 15.1f, 17f, 15.1f)
            lineTo(13f, 15.1f)
            lineTo(13f, 17f)
            lineTo(17f, 17f)
            curveTo(19.76f, 17f, 22f, 14.76f, 22f, 12f)
            curveTo(22f, 9.24f, 19.76f, 7f, 17f, 7f)
            close()
        }
    }

    val Tune: ImageVector by lazy {
        icon("Tune") {
            moveTo(3f, 17f)
            lineTo(3f, 19f)
            lineTo(9f, 19f)
            lineTo(9f, 17f)
            close()
            moveTo(3f, 5f)
            lineTo(3f, 7f)
            lineTo(13f, 7f)
            lineTo(13f, 5f)
            close()
            moveTo(13f, 21f)
            lineTo(13f, 19f)
            lineTo(21f, 19f)
            lineTo(21f, 17f)
            lineTo(13f, 17f)
            lineTo(13f, 15f)
            lineTo(11f, 15f)
            lineTo(11f, 21f)
            close()
            moveTo(7f, 9f)
            lineTo(7f, 11f)
            lineTo(3f, 11f)
            lineTo(3f, 13f)
            lineTo(7f, 13f)
            lineTo(7f, 15f)
            lineTo(9f, 15f)
            lineTo(9f, 9f)
            close()
            moveTo(21f, 13f)
            lineTo(21f, 11f)
            lineTo(11f, 11f)
            lineTo(11f, 13f)
            close()
            moveTo(15f, 9f)
            lineTo(17f, 9f)
            lineTo(17f, 7f)
            lineTo(21f, 7f)
            lineTo(21f, 5f)
            lineTo(17f, 5f)
            lineTo(17f, 3f)
            lineTo(15f, 3f)
            close()
        }
    }

    val Share: ImageVector by lazy {
        icon("Share") {
            moveTo(14f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 10f)
            lineTo(19f, 10f)
            lineTo(19f, 6.4f)
            lineTo(11.4f, 14f)
            lineTo(10f, 12.6f)
            lineTo(17.6f, 5f)
            lineTo(14f, 5f)
            close()
            moveTo(5f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 7f)
            lineTo(7f, 7f)
            lineTo(7f, 19f)
            lineTo(19f, 19f)
            lineTo(19f, 13f)
            lineTo(21f, 13f)
            lineTo(21f, 21f)
            lineTo(5f, 21f)
            close()
        }
    }

    /** A clock face: the ring and the two hands, the mark the app uses for history. */
    val History: ImageVector by lazy {
        icon("History") {
            circle(cx = 12f, cy = 12f, r = 9f)
            circle(cx = 12f, cy = 12f, r = 7f)
            moveTo(11.2f, 6.8f)
            lineTo(12.8f, 6.8f)
            lineTo(12.8f, 12.2f)
            lineTo(16.4f, 12.2f)
            lineTo(16.4f, 13.8f)
            lineTo(11.2f, 13.8f)
            close()
        }
    }

    /** A page with written lines, for the notes surface. */
    val Note: ImageVector by lazy {
        icon("Note") {
            moveTo(4f, 3f)
            lineTo(20f, 3f)
            lineTo(20f, 21f)
            lineTo(4f, 21f)
            close()
            moveTo(6f, 5f)
            lineTo(18f, 5f)
            lineTo(18f, 19f)
            lineTo(6f, 19f)
            close()
            moveTo(8f, 8f)
            lineTo(16f, 8f)
            lineTo(16f, 9.6f)
            lineTo(8f, 9.6f)
            close()
            moveTo(8f, 11.2f)
            lineTo(16f, 11.2f)
            lineTo(16f, 12.8f)
            lineTo(8f, 12.8f)
            close()
            moveTo(8f, 14.4f)
            lineTo(13f, 14.4f)
            lineTo(13f, 16f)
            lineTo(8f, 16f)
            close()
        }
    }

    /** A bin with its lid, for removing a saved row. */
    val Delete: ImageVector by lazy {
        icon("Delete") {
            moveTo(9f, 3f)
            lineTo(15f, 3f)
            lineTo(15f, 4.5f)
            lineTo(21f, 4.5f)
            lineTo(21f, 6f)
            lineTo(3f, 6f)
            lineTo(3f, 4.5f)
            lineTo(9f, 4.5f)
            close()
            moveTo(6f, 7f)
            lineTo(18f, 7f)
            lineTo(17f, 21f)
            lineTo(7f, 21f)
            close()
            moveTo(8f, 9f)
            lineTo(16f, 9f)
            lineTo(15.4f, 19f)
            lineTo(8.6f, 19f)
            close()
        }
    }

    // Four tiles: the channel grid's layout control when the grid is showing.
    val GridView: ImageVector by lazy {
        icon("GridView") {
            moveTo(3f, 3f)
            lineTo(11f, 3f)
            lineTo(11f, 11f)
            lineTo(3f, 11f)
            close()
            moveTo(13f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 11f)
            lineTo(13f, 11f)
            close()
            moveTo(3f, 13f)
            lineTo(11f, 13f)
            lineTo(11f, 21f)
            lineTo(3f, 21f)
            close()
            moveTo(13f, 13f)
            lineTo(21f, 13f)
            lineTo(21f, 21f)
            lineTo(13f, 21f)
            close()
        }
    }

    // Three full-width bars: the channel grid's layout control when the list is showing.
    val ListView: ImageVector by lazy {
        icon("ListView") {
            moveTo(3f, 4f)
            lineTo(21f, 4f)
            lineTo(21f, 8f)
            lineTo(3f, 8f)
            close()
            moveTo(3f, 10f)
            lineTo(21f, 10f)
            lineTo(21f, 14f)
            lineTo(3f, 14f)
            close()
            moveTo(3f, 16f)
            lineTo(21f, 16f)
            lineTo(21f, 20f)
            lineTo(3f, 20f)
            close()
        }
    }
}

/** A raised thumb: the wrist block, the thumb rising out of it and the folded fingers. */
private fun PathBuilder.thumbPath() {
    moveTo(2f, 10f)
    lineTo(7f, 10f)
    lineTo(7f, 21f)
    lineTo(2f, 21f)
    close()
    moveTo(9f, 21f)
    lineTo(9f, 10.5f)
    lineTo(12.5f, 3.5f)
    curveTo(14f, 3.4f, 15f, 4.4f, 15f, 6f)
    lineTo(15f, 9f)
    lineTo(20.4f, 9f)
    curveTo(21.8f, 9f, 22.6f, 10.1f, 22.3f, 11.4f)
    lineTo(20.4f, 19.3f)
    curveTo(20.1f, 20.4f, 19.1f, 21f, 18f, 21f)
    close()
}

private fun icon(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathBuilder = pathBuilder,
            )
        }.build()

/** One filled glyph from the SVG path data of the reference app's icon, verbatim. */
private fun pathIcon(
    name: String,
    pathData: String,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes(pathData),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            )
        }.build()

/** A circle as four cubic segments, so a ring can be cut out of it with [PathFillType.EvenOdd]. */
private fun PathBuilder.circle(
    cx: Float,
    cy: Float,
    r: Float,
) {
    val k = r * 0.5523f
    moveTo(cx, cy - r)
    curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
    curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
    curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
    curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
    close()
}
