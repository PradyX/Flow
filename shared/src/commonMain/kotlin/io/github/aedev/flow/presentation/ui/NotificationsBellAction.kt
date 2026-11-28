package io.github.aedev.flow.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aedev.flow.resources.Res
import io.github.aedev.flow.resources.notification_badge_more
import io.github.aedev.flow.resources.notifications_title
import io.github.aedev.flow.resources.notifications_unread
import org.jetbrains.compose.resources.stringResource

private val BadgeSize = 16.dp
private val BadgeOffsetX = 6.dp
private val BadgeOffsetY = (-4).dp
private const val MAX_COUNTED = 9

/**
 * The bar's bell with the app's unread disc: a 16 dp primary circle hanging off the icon's top end
 * carrying the unread count, or the cap once there are more than nine (`FlowTopBarActions.kt` in the
 * app draws `Icons.Outlined.Notifications` with the same disc, offset and 9 sp bold label). The disc
 * is decoration on top of the button's own label, so it is kept out of the accessibility tree and
 * the count is spoken by the button instead.
 */
@Composable
internal fun NotificationsBellAction(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    val label =
        if (unreadCount > 0) {
            stringResource(Res.string.notifications_unread, unreadCount)
        } else {
            stringResource(Res.string.notifications_title)
        }
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = FlowIcons.Bell,
                contentDescription = label,
            )
            if (unreadCount > 0) {
                Box(
                    modifier =
                        Modifier
                            .offset(x = BadgeOffsetX, y = BadgeOffsetY)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .size(BadgeSize)
                            .clearAndSetSemantics {},
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            if (unreadCount > MAX_COUNTED) {
                                stringResource(Res.string.notification_badge_more)
                            } else {
                                unreadCount.toString()
                            },
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 9.sp,
                            ),
                    )
                }
            }
        }
    }
}
