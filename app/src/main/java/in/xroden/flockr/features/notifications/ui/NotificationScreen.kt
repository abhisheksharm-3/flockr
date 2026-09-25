/** The inbox: new notifications first, each opening its subject, swiped away to delete. */
package `in`.xroden.flockr.features.notifications.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import `in`.xroden.flockr.features.notifications.presentation.NotificationUiState
import `in`.xroden.flockr.features.notifications.presentation.NotificationViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
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
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (ready != null && ready.unreadCount > 0) {
                        IconButton(onClick = { haptics.tap(); viewModel.markAllRead() }) { Icon(Icons.Rounded.DoneAll, contentDescription = "Mark all read") }
                    }
                    if (ready != null && ready.notifications.any { it.isRead }) {
                        IconButton(onClick = { haptics.tap(); viewModel.clearRead() }) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear read") }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                NotificationUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
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
    val (unread, read) = notifications.partition { !it.isRead }
    LazyColumn {
        listOf("New" to unread, "Earlier" to read).forEach { (title, group) ->
            if (group.isEmpty()) return@forEach
            item(key = title) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.lg, top = Spacing.lg, bottom = Spacing.xs),
                )
            }
            items(group, key = { it.id }) { notification ->
                val dismissState = rememberSwipeToDismissBoxState()
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)) },
                    onDismiss = { onDelete(notification) },
                    modifier = Modifier.animateItem(),
                ) {
                    NotificationRow(notification, onClick = { onOpen(notification) })
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: Notification, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(ComponentHeight.avatar), contentAlignment = Alignment.Center) {
                    Icon(groupIcon(notification.kind?.group), contentDescription = null, modifier = Modifier.size(IconSize.md))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(notification.title, style = if (notification.isRead) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyLargeEmphasized)
                Text(
                    notification.body,
                    style = MaterialTheme.typography.bodyMedium,
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
}

private fun groupIcon(group: NotificationGroup?): ImageVector = when (group) {
    NotificationGroup.MONEY -> Icons.Rounded.Payments
    NotificationGroup.CHORES -> Icons.Rounded.CleaningServices
    NotificationGroup.MESSAGES -> Icons.AutoMirrored.Rounded.Chat
    NotificationGroup.HOUSE, null -> Icons.Rounded.Home
}

/** "5 minutes ago", "Yesterday", or a date, in the device's language. */
private fun relativeTime(notification: Notification): String = DateUtils.getRelativeTimeSpanString(
    notification.createdAt.toEpochMilliseconds(),
    Clock.System.now().toEpochMilliseconds(),
    DateUtils.MINUTE_IN_MILLIS,
).toString()
