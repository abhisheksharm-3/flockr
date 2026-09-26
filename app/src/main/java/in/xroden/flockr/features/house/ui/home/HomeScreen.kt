/** The signed-in landing page: where the viewer stands across every house, what is due this week, and the houses themselves. */
package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.presentation.DueSoon
import `in`.xroden.flockr.features.house.presentation.HomeSummary
import `in`.xroden.flockr.features.house.presentation.HomeSummaryViewModel
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseEvent
import `in`.xroden.flockr.features.house.presentation.HouseListUiState
import `in`.xroden.flockr.features.house.presentation.ViewerPayment
import `in`.xroden.flockr.features.notifications.presentation.NotificationUiState
import `in`.xroden.flockr.features.notifications.presentation.NotificationViewModel
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroBackdrop
import `in`.xroden.flockr.ui.components.HouseMap
import `in`.xroden.flockr.ui.components.latLngOf
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroBadge
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroCountUp
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.balanceColor
import `in`.xroden.flockr.ui.components.balanceHeadline
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.dueLabel
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal
import java.time.LocalTime
import kotlinx.datetime.daysUntil

private const val MAX_BADGE_COUNT = 99
private const val AFTERNOON_STARTS_AT = 12
private const val EVENING_STARTS_AT = 17
private const val PAYMENTS_SHOWN = 3
private const val AVATARS_ON_TILE = 5
private const val TILE_ASPECT = 16f / 9f
private const val AVATAR_RING_ALPHA = 0.3f

@Composable
fun HomeScreen(
    onHouseClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateHouseClick: () -> Unit,
    onJoinHouseClick: () -> Unit,
    onAddExpense: (houseId: String) -> Unit,
    onSettleUp: (houseId: String, payment: SettleUpPayment) -> Unit,
    onOpenBills: (houseId: String) -> Unit,
    onOpenChores: (houseId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    summaryViewModel: HomeSummaryViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val invitations by viewModel.pendingInvitations.collectAsStateWithLifecycle()
    val respondingId by viewModel.respondingInvitationId.collectAsStateWithLifecycle()
    val summary by summaryViewModel.summary.collectAsStateWithLifecycle()
    val notificationState by notificationViewModel.state.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val unreadCount = (notificationState as? NotificationUiState.Ready)?.unreadCount ?: 0
    val firstName = (profileState as? ProfileUiState.Success)?.profile?.fullName
        ?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    val greeting = remember { greetingForNow() }
    val houses = (uiState as? HouseListUiState.Success)?.houses

    LaunchedEffect(Unit) { viewModel.loadPendingInvitations() }
    LaunchedEffect(houses) { houses?.let(summaryViewModel::load) }
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val current = uiState) {
            HouseListUiState.Loading -> SkeletonHeroScreen()
            is HouseListUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) { ErrorState(current.message, onRetry = viewModel::refresh) }
            is HouseListUiState.Success -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val list = rememberLazyListState()
                Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                    item(key = "hero") {
                        GreetingHero(
                            title = firstName?.let { "$greeting, $it" } ?: greeting,
                            houses = current.houses,
                            summary = summary,
                            unreadCount = unreadCount,
                            onNotificationsClick = onNotificationsClick,
                            onSettingsClick = onSettingsClick,
                            onAddExpense = onAddExpense,
                            onSettleUp = onSettleUp,
                        )
                    }
                    if (invitations.isNotEmpty()) {
                        item(key = "invitations_title") { SectionTitle("Invitations") }
                        items(invitations, key = { "invitation_${it.id}" }) { invitation ->
                            InvitationBand(
                                invitation = invitation,
                                isResponding = respondingId != null,
                                onAccept = { viewModel.acceptInvitation(invitation.id) },
                                onDecline = { viewModel.rejectInvitation(invitation.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    val dueSoon = summary?.dueSoon.orEmpty()
                    if (dueSoon.isNotEmpty()) {
                        item(key = "due_title") { SectionTitle("This week") }
                        items(dueSoon, key = { dueKey(it) }) { item ->
                            DueSoonRow(item, onOpenBills = onOpenBills, onOpenChores = onOpenChores)
                        }
                    }
                    if (current.houses.isEmpty()) {
                        item(key = "empty") { NoHousesYet(onCreateHouseClick, onJoinHouseClick) }
                    } else {
                        item(key = "houses_title") { SectionTitle(if (current.houses.size == 1) "Your house" else "Your houses") }
                        items(current.houses, key = { it.id }) { house ->
                            HouseTile(
                                house = house,
                                members = summary?.membersByHouse?.get(house.id).orEmpty(),
                                onClick = { onHouseClick(house.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(key = "another") { AnotherHouse(onCreateHouseClick, onJoinHouseClick) }
                    }
                    item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
                }
                HeroStatusBarScrim(isHeroGone = list.isHeroScrolledAway)
                }
            }
        }
    }
}

/**
 * The greeting, then the viewer's standing summed across houses and the people behind it. The sum
 * only means something in one currency, so with houses in several the hero lists people instead.
 */
@Composable
private fun GreetingHero(
    title: String,
    houses: List<HouseCardData>,
    summary: HomeSummary?,
    unreadCount: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddExpense: (String) -> Unit,
    onSettleUp: (String, SettleUpPayment) -> Unit,
) {
    HeroHeader(
        title = title,
        actions = {
            IconButton(onClick = onNotificationsClick) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(containerColor = MaterialTheme.flockrColors.sun, contentColor = MaterialTheme.flockrColors.onSun) {
                                Text(if (unreadCount > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else "$unreadCount")
                            }
                        }
                    }
                ) {
                    AnimatedGlyph(
                        Icons.Rounded.Notifications,
                        trigger = unreadCount,
                        motion = GlyphMotion.WIGGLE,
                        contentDescription = if (unreadCount > 0) "Notifications, $unreadCount unread" else "Notifications",
                    )
                }
            }
            IconButton(onClick = onSettingsClick) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
        },
    ) {
        if (houses.isEmpty()) {
            HeroLabel("Welcome to Flockr")
            HeroCaption("Split bills, share chores and keep the shopping list in one place.")
            return@HeroHeader
        }
        val currencies = houses.map { it.currencyCode }.distinct()
        val payments = summary?.payments.orEmpty()
        if (currencies.size == 1) {
            val net = houses.fold(BigDecimal.ZERO) { sum, house -> sum + house.myNet }
            HeroLabel(if (houses.size == 1 || net.signum() == 0) balanceHeadline(net) else "${balanceHeadline(net)} across ${houses.size} houses")
            if (net.signum() == 0) HeroBadge("All square") else HeroCountUp(net.abs(), currencies.single())
        } else {
            HeroLabel("You're in ${houses.size} houses")
        }
        payments.take(PAYMENTS_SHOWN).forEach { PaymentLine(it, showHouse = houses.size > 1) }
        if (payments.size > PAYMENTS_SHOWN) HeroCaption("and ${payments.size - PAYMENTS_SHOWN} more inside your houses")
        if (payments.isEmpty() && summary != null) {
            HeroCaption(if (houses.all { it.memberCount == 1 }) "Invite your housemates to start splitting." else "Nobody owes anybody. Nice.")
        }
        HeroActions {
            payments.firstOrNull()?.let { first -> HeroButton("Settle up", onClick = { onSettleUp(first.house.id, first.payment) }) }
            houses.singleOrNull()?.let { only ->
                if (payments.isEmpty()) HeroButton("Add expense", onClick = { onAddExpense(only.id) })
                else HeroSecondaryButton("Add expense", onClick = { onAddExpense(only.id) })
            }
        }
    }
}

/** "Karan owes you ₹560.01", with their face, and the house when there is more than one. */
@Composable
private fun PaymentLine(item: ViewerPayment, showHouse: Boolean) {
    val name = item.other?.shortName ?: "A former housemate"
    val amount = item.payment.amount.formatMoney(item.house.currencyCode)
    Row(
        modifier = Modifier.padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        MemberAvatar(
            name = item.other?.displayName ?: "?",
            avatarUrl = item.other?.avatarUrl,
            size = ComponentHeight.chip,
            modifier = Modifier.border(Spacing.xxs, MaterialTheme.flockrColors.onHero.copy(alpha = AVATAR_RING_ALPHA), CircleShape),
        )
        Text(
            (if (item.isOwedToViewer) "$name owes you $amount" else "You owe $name $amount") + if (showHouse) " · ${item.house.name}" else "",
            style = MaterialTheme.typography.bodyMediumEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun dueKey(item: DueSoon): String = when (item) {
    is DueSoon.BillDue -> "bill_${item.bill.id}"
    is DueSoon.ChoreDue -> "chore_${item.chore.id}"
}

@Composable
private fun DueSoonRow(item: DueSoon, onOpenBills: (String) -> Unit, onOpenChores: (String) -> Unit) {
    val where = " · ${item.house.name}"
    when (item) {
        is DueSoon.BillDue -> ListRow(
            headline = item.bill.name,
            supporting = dueLabel(item.bill.daysUntilDue) + where,
            leading = { IconBadge(Icons.AutoMirrored.Rounded.ReceiptLong, if (item.bill.daysUntilDue < 0) BadgeTone.ROSE else BadgeTone.SUN) },
            trailing = { TrailingAmount(item.bill.amount.formatMoney(item.house.currencyCode)) },
            onClick = { onOpenBills(item.house.id) },
        )
        is DueSoon.ChoreDue -> ListRow(
            headline = item.chore.taskName,
            supporting = "Your turn" + (item.chore.dueDate?.let { " · ${dueLabel(item.today.daysUntil(it))}" } ?: "") + where,
            leading = { IconBadge(Icons.Rounded.CleaningServices, BadgeTone.JADE) },
            onClick = { onOpenChores(item.house.id) },
        )
    }
}

/**
 * A house as its photo, rounded inside the page margins with housemates' faces tucked over its
 * bottom edge, and the name and the viewer's balance in plain text underneath. Nothing sits on the
 * photo itself, so any picture works. A house without a photo shows its street on a map, or cobalt with
 * the flock and a house mark when it has no location either.
 */
@Composable
private fun HouseTile(house: HouseCardData, members: List<MemberWithProfile>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xs, bottom = Spacing.lg),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().aspectRatio(TILE_ASPECT).clip(MaterialTheme.shapes.large), contentAlignment = Alignment.Center) {
                val location = latLngOf(house.latitude, house.longitude)
                when {
                    !house.headerImageUrl.isNullOrBlank() -> AsyncImage(
                        model = house.headerImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    location != null -> {
                        HouseMap(location, Modifier.matchParentSize())
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(IconSize.xl).offset(y = -IconSize.xl / 2),
                        )
                    }
                    else -> HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
                }
                if (house.headerImageUrl.isNullOrBlank() && location == null) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = MaterialTheme.flockrColors.onHeroVariant, modifier = Modifier.size(IconSize.xxl))
                }
            }
            if (members.isNotEmpty()) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = Spacing.md).offset(y = ComponentHeight.chip / 2),
                    horizontalArrangement = Arrangement.spacedBy(-Spacing.sm),
                ) {
                    members.take(AVATARS_ON_TILE).forEach { member ->
                        MemberAvatar(
                            name = member.displayName,
                            avatarUrl = member.avatarUrl,
                            size = ComponentHeight.chip,
                            modifier = Modifier.border(Spacing.xxs, MaterialTheme.colorScheme.background, CircleShape),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = if (members.isEmpty()) Spacing.md else Spacing.xl),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(Modifier.weight(1f)) {
                Text(house.name, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (house.memberCount == 1) "Just you" else "${house.memberCount} housemates"} · ${house.monthlySpendLabel} this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val net = house.myNet
            if (net.signum() == 0) TrailingAmount("All square")
            else TrailingAmount(net.abs().formatMoney(house.currencyCode), if (net.signum() > 0) "you're owed" else "you owe", balanceColor(net))
        }
    }
}

@Composable
private fun houseTint(seed: String): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (seed.hashCode().mod(3)) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> MaterialTheme.flockrColors.sun to MaterialTheme.flockrColors.onSun
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
}

@Composable
private fun AnotherHouse(onCreateHouseClick: () -> Unit, onJoinHouseClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        OutlinedButton(onClick = onCreateHouseClick) {
            Icon(Icons.Rounded.AddHome, contentDescription = null, modifier = Modifier.size(IconSize.sm))
            Text("New house", modifier = Modifier.padding(start = Spacing.sm))
        }
        OutlinedButton(onClick = onJoinHouseClick) {
            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(IconSize.sm))
            Text("Join with a code", modifier = Modifier.padding(start = Spacing.sm))
        }
    }
}

@Composable
private fun NoHousesYet(onCreateHouseClick: () -> Unit, onJoinHouseClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("Start with your house", style = MaterialTheme.typography.headlineSmallEmphasized)
        Text(
            "Create one for the people you live with, or join with the code a housemate sent you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCreateHouseClick, modifier = Modifier.fillMaxWidth()) { Text("Create a house") }
        OutlinedButton(onClick = onJoinHouseClick, modifier = Modifier.fillMaxWidth()) { Text("Join with a code") }
    }
}

/** An invitation waits on the viewer, so it sits on a full-width tinted band rather than a floating card. */
@Composable
private fun InvitationBand(
    invitation: InvitationWithHouse,
    isResponding: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            HouseImage(
                imageUrl = invitation.headerImageUrl,
                seed = invitation.houseId,
                modifier = Modifier.size(ComponentHeight.avatarLarge).clip(MaterialTheme.shapes.medium),
            )
            Column(Modifier.weight(1f)) {
                Text(invitation.houseName, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${invitation.inviterName} invited you to join", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End)) {
            TextButton(onClick = { haptics.tap(); onDecline() }, enabled = !isResponding) { Text("Decline") }
            Button(onClick = { haptics.tap(); onAccept() }, enabled = !isResponding) { Text("Join ${invitation.houseName}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/**
 * A house's header photo, or a placeholder tinted by [seed] so houses without a photo still look
 * different from each other.
 */
@Composable
internal fun HouseImage(imageUrl: String?, seed: String, modifier: Modifier = Modifier) {
    val (container, content) = houseTint(seed)
    Box(modifier.background(container), contentAlignment = Alignment.Center) {
        if (imageUrl.isNullOrBlank()) {
            Icon(Icons.Rounded.Home, contentDescription = null, tint = content, modifier = Modifier.size(IconSize.md))
        } else {
            AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
