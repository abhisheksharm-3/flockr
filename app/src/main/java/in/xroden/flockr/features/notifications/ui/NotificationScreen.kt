/** The inbox: new notifications first, each opening its subject, swiped away to delete. */
package `in`.xroden.flockr.features.notifications.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationGroup
import `in`.xroden.flockr.features.notifications.model.NotificationType
import `in`.xroden.flockr.features.notifications.presentation.NotificationUiState
import `in`.xroden.flockr.features.notifications.presentation.NotificationViewModel
import `in`.xroden.flockr.ui.components.SkeletonRows
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics
import kotlin.time.Clock

@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    onOpen: (Notification) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val ready = state as? NotificationUiState.Ready

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlockrTopAppBar(
                title = "Notifications",
                subtitle = ready?.unreadCount?.takeIf { it > 0 }?.let { "$it new" },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (ready != null && ready.unreadCount > 0) {
                        IconButton(onClick = { haptics.tap(); viewModel.markAllRead() }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.DoneAll, contentDescription = "Mark all read") }
                    }
                    if (ready != null && ready.notifications.any { it.isRead }) {
                        IconButton(onClick = { haptics.tap(); viewModel.clearRead() }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear read") }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when (val current = state) {
                NotificationUiState.Loading -> SkeletonRows()
                is NotificationUiState.Error -> ErrorState(current.message, onRetry = viewModel::load)
                is NotificationUiState.Ready -> if (current.notifications.isEmpty()) {
                    EmptyState(icon = Icons.Rounded.Notifications, title = "You're all caught up", subtitle = "Expenses, bills, chores and messages that involve you show up here.")
                } else {
                    Inbox(
                        notifications = current.notifications,
                        onOpen = { notification ->
                            viewModel.open(notification)
                            onOpen(notification)
                        },
                        onDelete = { haptics.gestureEnd(); viewModel.delete(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Inbox(notifications: List<Notification>, onOpen: (Notification) -> Unit, onDelete: (Notification) -> Unit) {
    val haptics = rememberHaptics()
    val (unread, read) = notifications.partition { !it.isRead }
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxl)) {
        listOf("New" to unread, "Earlier" to read).forEach { (title, group) ->
            if (group.isEmpty()) return@forEach
            item(key = title) { SectionTitle(title) }
            items(group, key = { it.id }) { notification ->
                val dismissState = rememberSwipeToDismissBoxState()
                val isArmed = dismissState.targetValue != SwipeToDismissBoxValue.Settled
                LaunchedEffect(isArmed) { if (isArmed) haptics.gestureThreshold() }
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { DeleteBackground(fromStart = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd, isArmed = isArmed) },
                    onDismiss = { onDelete(notification) },
                    modifier = Modifier.animateItem(),
                ) {
                    NotificationRow(notification, onClick = { onOpen(notification) })
                }
            }
        }
        item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

/** What shows under a row as it is swiped away: the error colour, and a bin on the side it is leaving from that shakes once letting go would delete. */
@Composable
private fun DeleteBackground(fromStart: Boolean, isArmed: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = Spacing.xl),
        contentAlignment = if (fromStart) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        AnimatedGlyph(Icons.Rounded.Delete, trigger = isArmed, motion = GlyphMotion.WIGGLE, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

/** The row is opaque so the delete colour only shows where it has been swiped aside. Unread rows read heavier and carry a dot. */
@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    val (icon, tone) = kindBadge(notification.kind)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        IconBadge(icon, tone)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                notification.title,
                style = if (notification.isRead) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleSmallEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(relativeTime(notification), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!notification.isRead) {
            Box(Modifier.padding(top = Spacing.xs).size(Spacing.sm).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

/** The list is filed under chores for muting, but reads as shopping, so it gets the cart. */
private fun kindBadge(kind: NotificationType?): Pair<ImageVector, BadgeTone> = if (kind == NotificationType.SHOPPING_ITEM_ADDED) {
    Icons.Rounded.ShoppingCart to BadgeTone.SUN
} else when (kind?.group) {
    NotificationGroup.MONEY -> Icons.Rounded.Payments to BadgeTone.JADE
    NotificationGroup.CHORES -> Icons.Rounded.CleaningServices to BadgeTone.SUN
    NotificationGroup.MESSAGES -> Icons.AutoMirrored.Rounded.Chat to BadgeTone.COBALT
    NotificationGroup.HOUSE, null -> Icons.Rounded.Home to BadgeTone.SLATE
}

/** "5 minutes ago", "Yesterday", or a date, in the device's language. */
private fun relativeTime(notification: Notification): String = DateUtils.getRelativeTimeSpanString(
    notification.createdAt.toEpochMilliseconds(),
    Clock.System.now().toEpochMilliseconds(),
    DateUtils.MINUTE_IN_MILLIS,
).toString()
