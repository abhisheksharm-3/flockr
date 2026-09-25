/** The signed-in landing page: invitations waiting for an answer, then every house the user is in. */
package `in`.xroden.flockr.features.house.ui.home

import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.ui.components.balanceColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseEvent
import `in`.xroden.flockr.features.house.presentation.HouseListUiState
import `in`.xroden.flockr.features.notifications.presentation.NotificationUiState
import `in`.xroden.flockr.features.notifications.presentation.NotificationViewModel
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FabAction
import `in`.xroden.flockr.ui.components.buttons.FlockrFabMenu
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics
import java.time.LocalTime

private const val MAX_BADGE_COUNT = 99
private const val AFTERNOON_STARTS_AT = 12
private const val EVENING_STARTS_AT = 17

@Composable
fun HomeScreen(
    onHouseClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateHouseClick: () -> Unit,
    onJoinHouseClick: () -> Unit,
    onNavigateToJoinPreview: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val invitations by viewModel.pendingInvitations.collectAsStateWithLifecycle()
    val respondingId by viewModel.respondingInvitationId.collectAsStateWithLifecycle()
    val notificationState by notificationViewModel.state.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val unreadCount = (notificationState as? NotificationUiState.Ready)?.unreadCount ?: 0
    val firstName = (profileState as? ProfileUiState.Success)?.profile?.fullName
        ?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    val greeting = remember { greetingForNow() }

    LaunchedEffect(Unit) { viewModel.loadPendingInvitations() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HouseEvent.Joined -> {
                    haptics.success()
                    snackbarHostState.showSnackbar("You joined ${event.houseName}")
                }
                is HouseEvent.Failed -> {
                    haptics.error()
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlockrTopAppBar(
                title = firstName?.let { "$greeting, $it" } ?: greeting,
                onNavigateBack = null,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(if (unreadCount > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else "$unreadCount") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.Notifications,
                                contentDescription = if (unreadCount > 0) "Notifications, $unreadCount unread" else "Notifications"
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = uiState) {
                HouseListUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is HouseListUiState.Error -> ErrorState(current.message, onRetry = viewModel::refresh)
                is HouseListUiState.Success -> PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    HomeContent(
                        houses = current.houses,
                        invitations = invitations,
                        respondingId = respondingId,
                        onHouseClick = onHouseClick,
                        onCreateHouseClick = onCreateHouseClick,
                        onAccept = viewModel::acceptInvitation,
                        onDecline = viewModel::rejectInvitation,
                    )
                }
            }
            FlockrFabMenu(
                actions = listOf(
                    FabAction("Create a house", Icons.Rounded.AddHome, onCreateHouseClick),
                    FabAction("Join with a code", Icons.Rounded.Key, onJoinHouseClick),
                ),
                contentDescription = "Add a house",
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun HomeContent(
    houses: List<HouseCardData>,
    invitations: List<InvitationWithHouse>,
    respondingId: String?,
    onHouseClick: (String) -> Unit,
    onCreateHouseClick: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.xxxxl * 2),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (invitations.isNotEmpty()) {
            item(key = "invitations_header") { SectionLabel("Invitations") }
            items(invitations, key = { "invitation_${it.id}" }) { invitation ->
                InvitationCard(
                    invitation = invitation,
                    isResponding = respondingId != null,
                    onAccept = { onAccept(invitation.id) },
                    onDecline = { onDecline(invitation.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (houses.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Home,
                    title = "No houses yet",
                    subtitle = "Create a house for the people you live with, or join one with the code a housemate sent you.",
                    actionText = "Create a house",
                    onActionClick = onCreateHouseClick,
                )
            }
        } else {
            if (invitations.isNotEmpty()) {
                item(key = "houses_header") { SectionLabel("Your houses") }
            }
            items(houses, key = { it.id }) { house ->
                HouseCard(house = house, onClick = { onHouseClick(house.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

@Composable
private fun InvitationCard(
    invitation: InvitationWithHouse,
    isResponding: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                HouseImage(
                    imageUrl = invitation.headerImageUrl,
                    seed = invitation.houseId,
                    modifier = Modifier.size(ComponentHeight.avatarLarge).clip(MaterialTheme.shapes.large),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        invitation.houseName,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${invitation.inviterName} invited you to join",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)) {
                TextButton(onClick = { haptics.tap(); onDecline() }, enabled = !isResponding) { Text("Decline") }
                Button(onClick = { haptics.tap(); onAccept() }, enabled = !isResponding) { Text("Accept") }
            }
        }
    }
}

@Composable
private fun HouseCard(house: HouseCardData, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (!house.headerImageUrl.isNullOrBlank()) {
            HouseImage(imageUrl = house.headerImageUrl, seed = house.id, modifier = Modifier.fillMaxWidth().height(ComponentHeight.cardSmall))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (house.headerImageUrl.isNullOrBlank()) {
                HouseImage(imageUrl = null, seed = house.id, modifier = Modifier.size(ComponentHeight.avatarLarge).clip(MaterialTheme.shapes.large))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    house.name,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (house.memberCount == 1) "1 member" else "${house.memberCount} members",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val net = house.myNet
                Text(
                    when (net.signum()) {
                        1 -> "you're owed"
                        -1 -> "you owe"
                        else -> "settled up"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = balanceColor(net),
                )
                if (net.signum() != 0) {
                    Text(net.abs().formatMoney(house.currencyCode), style = MaterialTheme.typography.titleMediumEmphasized, color = balanceColor(net))
                }
                Text("${house.monthlySpendLabel} this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * A house's header photo, or a placeholder tinted by [seed] so houses without a photo still look
 * different from each other.
 */
@Composable
internal fun HouseImage(imageUrl: String?, seed: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (seed.hashCode().mod(3)) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Box(modifier.background(container), contentAlignment = Alignment.Center) {
        if (imageUrl.isNullOrBlank()) {
            Icon(Icons.Rounded.Home, contentDescription = null, tint = content, modifier = Modifier.size(IconSize.lg))
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun greetingForNow(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < AFTERNOON_STARTS_AT -> "Good morning"
        hour < EVENING_STARTS_AT -> "Good afternoon"
        else -> "Good evening"
    }
}
