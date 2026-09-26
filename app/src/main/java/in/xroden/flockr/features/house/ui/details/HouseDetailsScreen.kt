/**
 * A house's home: the house itself as the backdrop, its people and where the viewer stands with each,
 * a swipeable stack of what needs the viewer now, and a sheet that pulls up into the rest of the house.
 */
package `in`.xroden.flockr.features.house.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.expenses.ui.ledger.ExpenseRow
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.presentation.HouseDetailUiState
import `in`.xroden.flockr.features.house.presentation.HouseDetailsViewModel
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.HeroBackdrop
import `in`.xroden.flockr.ui.components.latLngOf
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.LightStatusBarIcons
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.SkeletonHubScreen
import `in`.xroden.flockr.ui.components.Shortcut
import `in`.xroden.flockr.ui.components.ShortcutPills
import `in`.xroden.flockr.ui.components.balanceColor
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.dueLabel
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.rememberHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.datetime.daysUntil

private val PromptHeight = 176.dp
private val FaceWidth = 88.dp
private const val DRAWER_OPEN_THRESHOLD = 0.35f
private const val PARALLAX = 0.5f
private val FaceRing = 3.dp
private const val FACE_RING_ALPHA = 0.35f
private const val ON_HERO_TINT_ALPHA = 0.14f
private const val DOT_IDLE_ALPHA = 0.35f

/** One thing that needs the viewer, as a page of the prompt stack, with the one action that does it. */
private data class Prompt(
    val topic: String,
    val title: String,
    val body: String,
    val action: String,
    val onAction: () -> Unit,
    val secondary: Pair<String, () -> Unit>? = null,
)

@Composable
fun HouseDetailsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToChores: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToManageMembers: () -> Unit,
    onNavigateToHouseSettings: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToBalances: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onSettleUp: (SettleUpPayment) -> Unit,
    viewModel: HouseDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    when (val current = state) {
        HouseDetailUiState.Loading -> SkeletonHubScreen()
        is HouseDetailUiState.Error -> Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) { ErrorState(current.message, onRetry = { viewModel.load(houseId) }) }
        }
        is HouseDetailUiState.Ready -> HouseHome(
            state = current,
            config = config,
            prompts = prompts(
                state = current,
                currencyCode = config.currency(),
                onSettleUp = onSettleUp,
                onBalances = onNavigateToBalances,
                onBills = onNavigateToBills,
                onChores = onNavigateToChores,
                onShopping = onNavigateToShopping,
                onAddExpense = onAddExpense,
            ),
            places = listOf(
                Shortcut("Expense", Icons.Rounded.Add, onAddExpense, isPrimary = true),
                Shortcut("Money", Icons.Rounded.AccountBalanceWallet, onNavigateToExpenses),
                Shortcut("Shopping", Icons.Rounded.ShoppingCart, onNavigateToShopping, current.digest.toBuy),
                Shortcut("Chores", Icons.Rounded.CleaningServices, onNavigateToChores, current.myChores.size),
                Shortcut("Chat", Icons.Rounded.Forum, onNavigateToChat),
                Shortcut("Documents", Icons.Rounded.Description, onNavigateToDocuments),
                Shortcut("Members", Icons.Rounded.Group, onNavigateToManageMembers),
            ),
            onNavigateBack = onNavigateBack,
            onOpenSettings = onNavigateToHouseSettings,
            onOpenExpense = onOpenExpense,
            onSeeAll = onNavigateToExpenses,
            onChat = onNavigateToChat,
            onMembers = onNavigateToManageMembers,
            onSettleUp = onSettleUp,
            onBalances = onNavigateToBalances,
        )
    }
}

/**
 * The house stays put behind everything and the sheet is a drawer over it. The drawer rests where
 * the backdrop's content ends and opens to the top; let go between the two and it settles at the
 * nearer one, so it is always either resting or open, never parked halfway. As it rises, the
 * backdrop content drifts up at half speed and fades, so it reads as passing under the sheet.
 * Past fully open the sheet's own content scrolls. The sheet is at least a screen tall, so the
 * house never shows beneath a short one.
 */
@Composable
private fun HouseHome(
    state: HouseDetailUiState.Ready,
    config: HouseConfig?,
    prompts: List<Prompt>,
    places: List<Shortcut>,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExpense: (String) -> Unit,
    onSeeAll: () -> Unit,
    onChat: () -> Unit,
    onMembers: () -> Unit,
    onSettleUp: (SettleUpPayment) -> Unit,
    onBalances: () -> Unit,
) {
    LightStatusBarIcons()
    var person by remember { mutableStateOf<MemberWithProfile?>(null) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val openness by remember {
        derivedStateOf {
            val backdrop = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
            when {
                list.firstVisibleItemIndex > 0 || backdrop == null -> 1f
                backdrop.size == 0 -> 0f
                else -> (list.firstVisibleItemScrollOffset.toFloat() / backdrop.size).coerceIn(0f, 1f)
            }
        }
    }

    val haptics = rememberHaptics()
    LaunchedEffect(list) {
        snapshotFlow { list.isScrollInProgress }.collect { scrolling ->
            if (!scrolling && list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset > 0) {
                list.animateScrollToItem(if (openness > DRAWER_OPEN_THRESHOLD) 1 else 0)
                haptics.gestureEnd()
            }
        }
    }
    LaunchedEffect(list) {
        snapshotFlow { openness > DRAWER_OPEN_THRESHOLD }.drop(1).distinctUntilChanged().collect { if (list.isScrollInProgress) haptics.gestureThreshold() }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        HeroBackdrop(state.house.headerImageUrl, Modifier.fillMaxSize(), location = latLngOf(state.house.latitude, state.house.longitude))
        LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
            item(key = "backdrop") {
                Column(
                    Modifier
                        .graphicsLayer {
                            translationY = list.firstVisibleItemScrollOffset * PARALLAX
                            alpha = 1f - openness
                        }
                        .statusBarsPadding(),
                ) {
                    TopActions(onNavigateBack, onOpenSettings)
                    HouseTitle(state)
                    Faces(state, config.currency(), onPick = { haptics.tap(); person = it }, onInvite = onMembers)
                    PromptStack(prompts)
                }
            }
            item(key = "sheet") {
                HouseSheet(
                    state, config, places, onOpenExpense, onSeeAll, onChat, onMembers,
                    onAddPhoto = onOpenSettings,
                    openness = openness,
                    onHandleClick = { scope.launch { list.animateScrollToItem(if (openness > DRAWER_OPEN_THRESHOLD) 0 else 1) } },
                    modifier = Modifier.heightIn(min = screenHeight),
                )
            }
        }
        HeroStatusBarScrim(isHeroGone = list.isHeroScrolledAway)
    }

    person?.let { picked ->
        PersonSheet(
            person = picked,
            state = state,
            currencyCode = config.currency(),
            onDismiss = { person = null },
            onSettleUp = onSettleUp,
            onBalances = onBalances,
            onChat = onChat,
        )
    }
}

@Composable
private fun TopActions(onNavigateBack: () -> Unit, onOpenSettings: () -> Unit) {
    val onHero = MaterialTheme.flockrColors.onHero
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = "House settings", tint = onHero) }
    }
}

@Composable
private fun HouseTitle(state: HouseDetailUiState.Ready) {
    val colors = MaterialTheme.flockrColors
    Column(Modifier.padding(horizontal = Spacing.xl)) {
        Text(state.house.name, style = MaterialTheme.typography.displaySmallEmphasized, color = colors.onHero, maxLines = 2, overflow = TextOverflow.Ellipsis)
        state.house.address?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onHeroVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Everyone else in the house, each with where the viewer stands with them according to the
 * settle-up plan. A housemate the plan pairs with no payment reads "nothing to settle", which is
 * what the plan says, rather than "square", which would claim more than it knows.
 *
 * However big the house, the row scrolls sideways and snaps face by face like the prompt stack,
 * with the next face peeking in at the edge. Anyone with money pending comes first, so the people
 * who matter right now are never scrolled out of view.
 */
@Composable
private fun Faces(state: HouseDetailUiState.Ready, currencyCode: String, onPick: (MemberWithProfile) -> Unit, onInvite: () -> Unit) {
    val colors = MaterialTheme.flockrColors
    val row = rememberLazyListState()
    val others = state.activeMembers.filter { it.userId != state.viewerId }.sortedBy { paymentWith(state, it) == null }
    LazyRow(
        state = row,
        flingBehavior = rememberSnapFlingBehavior(row),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(others, key = { it.userId }) { member ->
            val payment = paymentWith(state, member)
            val line = when {
                payment == null -> "nothing to settle"
                payment.toUserId == state.viewerId -> "owes you ${payment.amount.formatMoney(currencyCode)}"
                else -> "you owe ${payment.amount.formatMoney(currencyCode)}"
            }
            Face(member.shortName, line, if (payment != null) colors.sun else colors.onHeroVariant, onClick = { onPick(member) }) {
                MemberAvatar(
                    name = member.displayName,
                    avatarUrl = member.avatarUrl,
                    size = ComponentHeight.avatarLarge,
                    modifier = Modifier.border(FaceRing, colors.onHero.copy(alpha = FACE_RING_ALPHA), CircleShape),
                )
            }
        }
        item(key = "invite") {
            Face("Invite", "add someone", colors.onHeroVariant, onClick = onInvite) {
                Surface(shape = CircleShape, color = colors.onHero.copy(alpha = ON_HERO_TINT_ALPHA), contentColor = colors.onHero, modifier = Modifier.size(ComponentHeight.avatarLarge)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PersonAdd, contentDescription = null) }
                }
            }
        }
    }
}

private fun paymentWith(state: HouseDetailUiState.Ready, member: MemberWithProfile): SettleUpPayment? =
    state.viewerPayments.firstOrNull { it.fromUserId == member.userId || it.toUserId == member.userId }

@Composable
private fun Face(title: String, line: String, lineColor: Color, onClick: () -> Unit, avatar: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .width(FaceWidth)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $line" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        avatar()
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.flockrColors.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(line, style = MaterialTheme.typography.labelMedium, color = lineColor, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** What needs the viewer, one page at a time. A single page needs no dots. */
@Composable
private fun PromptStack(prompts: List<Prompt>) {
    val pager = rememberPagerState { prompts.size }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.drop(1).collect { haptics.select() } }
    Column {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(PromptHeight)) { index ->
            PromptPage(prompts[index], index, prompts.size)
        }
        if (prompts.size > 1) {
            Row(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                prompts.indices.forEach { index ->
                    val selected = pager.currentPage == index
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .clickable(onClickLabel = "Show prompt ${index + 1}") { scope.launch { pager.animateScrollToPage(index) } }
                            .padding(Spacing.xs),
                    ) {
                        Box(
                            Modifier
                                .size(width = if (selected) Spacing.xl else Spacing.sm, height = Spacing.sm)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.flockrColors.sun else MaterialTheme.flockrColors.onHero.copy(alpha = DOT_IDLE_ALPHA)),
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun PromptPage(prompt: Prompt, index: Int, count: Int) {
    val colors = MaterialTheme.flockrColors
    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.xl)) {
        Text(
            (if (count > 1) "${index + 1} of $count · " else "") + prompt.topic,
            style = MaterialTheme.typography.labelMediumEmphasized,
            color = colors.onHeroVariant,
        )
        Text(
            prompt.title,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = colors.onHero,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Text(
            prompt.body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onHeroVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            HeroButton(prompt.action, onClick = prompt.onAction)
            prompt.secondary?.let { (label, action) -> HeroSecondaryButton(label, onClick = action) }
        }
    }
}

/**
 * The prompts, most pressing first: money between the viewer and a housemate, then bills by how due
 * they are, then the viewer's chores, then the shopping list. With nothing pending, one calm page.
 */
private fun prompts(
    state: HouseDetailUiState.Ready,
    currencyCode: String,
    onSettleUp: (SettleUpPayment) -> Unit,
    onBalances: () -> Unit,
    onBills: () -> Unit,
    onChores: () -> Unit,
    onShopping: () -> Unit,
    onAddExpense: () -> Unit,
): List<Prompt> {
    val byId = state.members.associateBy { it.userId }
    val money = state.viewerPayments.map { payment ->
        val amount = payment.amount.formatMoney(currencyCode)
        if (payment.toUserId == state.viewerId) {
            Prompt("Money", "${byId.nameOf(payment.fromUserId, state.viewerId)} owes you $amount", "Record it once they've paid you.", "Record payment", { onSettleUp(payment) }, "All balances" to onBalances)
        } else {
            Prompt("Money", "You owe ${byId.nameOf(payment.toUserId, state.viewerId)} $amount", "Pay them however you like, then record it here.", "Settle up", { onSettleUp(payment) }, "All balances" to onBalances)
        }
    }
    val bills = state.upcomingBills.map { bill ->
        Prompt("Bills", "${bill.name} is ${dueLabel(bill.daysUntilDue).replaceFirstChar { it.lowercase() }}", "${bill.amount.formatMoney(currencyCode)} for the house.", "Open bills", onBills)
    }
    val chores = state.myChores.map { chore ->
        Prompt("Chores", "Your turn: ${chore.taskName}", chore.dueDate?.let { "${dueLabel(state.today.daysUntil(it))}." } ?: "Whenever suits you.", "Open chores", onChores)
    }
    val shopping = state.digest.toBuy?.takeIf { it > 0 }?.let { count ->
        listOf(Prompt("Shopping", if (count == 1) "1 thing on the list" else "$count things on the list", "Heading out? Take the list with you.", "Open the list", onShopping))
    }.orEmpty()
    return (money + bills + chores + shopping).ifEmpty {
        listOf(Prompt("All good", "Nothing needs you right now", "No bills due this week, and nobody owes anybody.", "Add expense", onAddExpense))
    }
}

@Composable
private fun HouseSheet(
    state: HouseDetailUiState.Ready,
    config: HouseConfig?,
    places: List<Shortcut>,
    onOpenExpense: (String) -> Unit,
    onSeeAll: () -> Unit,
    onChat: () -> Unit,
    onMembers: () -> Unit,
    onAddPhoto: () -> Unit,
    openness: Float,
    onHandleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corner = Spacing.xxxl * (1f - openness)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = corner, topEnd = corner),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = statusBar * openness, bottom = Spacing.xl)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = if (openness > DRAWER_OPEN_THRESHOLD) "Close the drawer" else "Open the drawer", onClick = onHandleClick)
                    .padding(top = Spacing.md, bottom = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(width = Spacing.xxxl, height = Spacing.xs).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
            }
            ShortcutPills(places)
            SectionTitle("Just happened", action = { TextButton(onClick = onSeeAll) { Text("See all") } })
            if (state.recent.isEmpty()) {
                Text(
                    "Nothing spent yet. Add the first expense and it shows up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            val byId = state.members.associateBy { it.userId }
        state.recent.forEach { expense -> ExpenseRow(expense, byId, state.viewerId, config.currency(), onClick = { onOpenExpense(expense.id) }) }
            SectionTitle("The house")
            ListRow(
                headline = "Chat",
                supporting = state.digest.lastMessage?.let { message ->
                    val sender = if (message.userId == state.viewerId) "You" else state.members.firstOrNull { it.userId == message.userId }?.shortName ?: message.senderName ?: "Someone"
                    "$sender: ${message.content}"
                } ?: "Say hello to the house",
                leading = { IconBadge(Icons.Rounded.Forum, BadgeTone.COBALT) },
                trailing = { Chevron() },
                onClick = onChat,
            )
            ListRow(
                headline = "Members",
                supporting = state.activeMembers.joinToString { it.shortName },
                leading = { IconBadge(Icons.Rounded.Group, BadgeTone.SLATE) },
                trailing = { Chevron() },
                onClick = onMembers,
            )
            if (state.house.headerImageUrl.isNullOrBlank() && state.canManageHouse) {
                ListRow(
                    headline = "Add a house photo",
                    supporting = "It becomes the backdrop of this page and the house's tile",
                    leading = { IconBadge(Icons.Rounded.AddAPhoto, BadgeTone.SUN) },
                    trailing = { Chevron() },
                    onClick = onAddPhoto,
                )
            }
        }
    }
}

/** One housemate, and what is between them and the viewer, with the action that fits. */
@Composable
private fun PersonSheet(
    person: MemberWithProfile,
    state: HouseDetailUiState.Ready,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSettleUp: (SettleUpPayment) -> Unit,
    onBalances: () -> Unit,
    onChat: () -> Unit,
) {
    val payment = paymentWith(state, person)
    val isOwedToViewer = payment?.toUserId == state.viewerId
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            MemberAvatar(name = person.displayName, avatarUrl = person.avatarUrl, size = IconSize.display)
            Text(person.displayName, style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
            Text(
                when {
                    payment == null -> "Nothing to settle between you two right now."
                    isOwedToViewer -> "${person.shortName} owes you ${payment.amount.formatMoney(currencyCode)}."
                    else -> "You owe ${person.shortName} ${payment.amount.formatMoney(currencyCode)}."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = payment?.let { balanceColor(if (isOwedToViewer) it.amount else it.amount.negate()) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            payment?.let {
                Button(onClick = { onDismiss(); onSettleUp(it) }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                    Text(if (isOwedToViewer) "Record their payment" else "Settle up with ${person.shortName}")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = { onDismiss(); onBalances() }, modifier = Modifier.weight(1f)) { Text("Shared history") }
                OutlinedButton(onClick = { onDismiss(); onChat() }, modifier = Modifier.weight(1f)) { Text("Message") }
            }
        }
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
