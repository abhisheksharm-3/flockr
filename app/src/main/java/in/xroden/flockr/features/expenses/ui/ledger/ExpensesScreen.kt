/** The Money hub: where you stand, the payments that would settle you up, and the house's activity by month. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import `in`.xroden.flockr.ui.components.SkeletonRow
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.utils.rememberHaptics
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.expenses.presentation.ExpensesUiState
import `in`.xroden.flockr.features.expenses.presentation.ExpensesViewModel
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroBadge
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroCountUp
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.Shortcut
import `in`.xroden.flockr.ui.components.ShortcutPills
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.balanceColor
import `in`.xroden.flockr.ui.components.balanceHeadline
import `in`.xroden.flockr.ui.components.buttons.FabAction
import `in`.xroden.flockr.ui.components.buttons.FlockrFabMenu
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.monthYearLabel
import java.math.BigDecimal

private const val CSV_MIME = "text/csv"

/** How many rows from the end the next page starts loading, so scrolling rarely waits. */
private const val LOAD_MORE_WITHIN = 8

/** Where the hub leads. Each is a navigation, so none carries a haptic of its own. */
data class ExpensesNavigation(
    val back: () -> Unit,
    val addExpense: () -> Unit,
    val openExpense: (expenseId: String) -> Unit,
    val settleUp: (fromUserId: String?, toUserId: String?, amount: BigDecimal?) -> Unit,
    val balances: () -> Unit,
    val bills: () -> Unit,
    val perDiem: () -> Unit,
    val reports: () -> Unit,
)

@Composable
fun ExpensesScreen(houseId: String, navigation: ExpensesNavigation, viewModel: ExpensesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_MIME)) { uri ->
        uri?.let { viewModel.export(houseId, it, context.contentResolver) }
    }
    val isNearEnd by remember {
        derivedStateOf { listState.layoutInfo.run { (visibleItemsInfo.lastOrNull()?.index ?: 0) >= totalItemsCount - LOAD_MORE_WITHIN } }
    }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(isNearEnd, state) { if (isNearEnd) viewModel.loadMore() }
    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            if (notice.isError) haptics.error() else haptics.success()
            snackbarHostState.showSnackbar(notice.message)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (val current = state) {
                ExpensesUiState.Loading -> SkeletonHeroScreen()
                is ExpensesUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                    ErrorState(message = current.message, onRetry = { viewModel.load(houseId) })
                }
                is ExpensesUiState.Ready -> ExpensesContent(
                    state = current,
                    currencyCode = config.currency(),
                    navigation = navigation,
                    listState = listState,
                    onSearch = viewModel::search,
                    onExport = { exporter.launch("Flockr expenses ${Clock.System.todayIn(TimeZone.currentSystemDefault())}.csv") },
                )
            }
            FlockrFabMenu(
                actions = listOf(
                    FabAction("Add expense", Icons.AutoMirrored.Rounded.ReceiptLong, navigation.addExpense),
                    FabAction("Record a payment", Icons.Rounded.Handshake) { navigation.settleUp(null, null, null) },
                ),
                contentDescription = "Add",
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding(),
            )
            if (state is ExpensesUiState.Ready) HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
        }
    }
}

@Composable
private fun ExpensesContent(
    state: ExpensesUiState.Ready,
    currencyCode: String,
    navigation: ExpensesNavigation,
    listState: LazyListState,
    onSearch: (String) -> Unit,
    onExport: () -> Unit,
) {
    val byMonth = state.expenses.groupBy { it.date.year to it.date.month }
    val payments = state.standing.paymentsOf(state.viewerId)
    var isSearching by rememberSaveable { mutableStateOf(state.search.isNotEmpty()) }
    var query by rememberSaveable { mutableStateOf(state.search) }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
        item(key = "hero") {
            StandingHero(
                net = state.standing.netOf(state.viewerId),
                payments = payments,
                viewerId = state.viewerId,
                currencyCode = currencyCode,
                navigation = navigation,
            )
        }
        item(key = "shortcuts") {
            ShortcutPills(
                listOf(
                    Shortcut("Bills", Icons.Rounded.EventRepeat, navigation.bills),
                    Shortcut("Usage", Icons.Rounded.WaterDrop, navigation.perDiem),
                    Shortcut("Reports", Icons.Rounded.BarChart, navigation.reports),
                    Shortcut("Balances", Icons.Rounded.Groups, navigation.balances),
                    Shortcut("Search", Icons.Rounded.Search, { isSearching = true }),
                    Shortcut("Export", Icons.Rounded.Download, onExport),
                ),
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
        if (isSearching) {
            item(key = "search") {
                FlockrTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    placeholder = "Search names and notes",
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { query = ""; onSearch(""); isSearching = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close search")
                        }
                    },
                    imeAction = ImeAction.Search,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
        }
        if (payments.isNotEmpty() && state.search.isEmpty()) {
            item(key = "settle_title") { SectionTitle("To settle up") }
            items(payments, key = { "pay_${it.fromUserId}_${it.toUserId}" }) { payment ->
                PaymentRow(payment, state, currencyCode, onSettle = { navigation.settleUp(payment.fromUserId, payment.toUserId, payment.amount) })
            }
        }
        if (state.expenses.isEmpty() && state.search.isNotEmpty()) {
            item(key = "no_match") {
                ListRow(
                    headline = "Nothing matches \"${state.search}\"",
                    supporting = "Search looks in each expense's name and notes.",
                    leading = { IconBadge(Icons.Rounded.Search, BadgeTone.SLATE) },
                )
            }
        } else if (state.expenses.isEmpty()) {
            item(key = "empty_title") { SectionTitle("Activity") }
            item(key = "empty") {
                ListRow(
                    headline = "No expenses yet",
                    supporting = "Add what you spend for the house and Flockr works out who owes whom.",
                    leading = { IconBadge(Icons.AutoMirrored.Rounded.ReceiptLong, BadgeTone.SUN) },
                    onClick = navigation.addExpense,
                )
            }
        }
        byMonth.forEach { (month, expenses) ->
            item(key = "month_${month.first}_${month.second}") { SectionTitle(expenses.first().date.monthYearLabel()) }
            items(expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    members = state.members,
                    viewerId = state.viewerId,
                    currencyCode = currencyCode,
                    onClick = { navigation.openExpense(expense.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (state.hasMore) item(key = "more") { SkeletonRow() }
        item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

/** The cobalt headline: what the viewer is owed or owes across the house, and the way to settle it. */
@Composable
private fun StandingHero(
    net: BigDecimal,
    payments: List<SettleUpPayment>,
    viewerId: String,
    currencyCode: String,
    navigation: ExpensesNavigation,
) {
    HeroHeader(title = "Money") {
        HeroLabel(balanceHeadline(net))
        if (net.signum() == 0) HeroBadge("All square") else HeroCountUp(net.abs(), currencyCode)
        HeroCaption(
            when (payments.size) {
                0 -> "Nobody owes anybody. Nice."
                1 -> "One payment settles you up."
                else -> "${payments.size} payments settle you up."
            },
        )
        HeroActions {
            payments.firstOrNull()?.let { payment ->
                HeroButton(
                    text = if (payment.fromUserId == viewerId) "Settle up" else "Record payment",
                    onClick = { navigation.settleUp(payment.fromUserId, payment.toUserId, payment.amount) },
                )
            }
            if (payments.isEmpty()) HeroButton("Add expense", onClick = navigation.addExpense)
        }
    }
}

/** A payment from the settle-up plan that involves the viewer; tapping it opens the payment, prefilled. */
@Composable
private fun PaymentRow(payment: SettleUpPayment, state: ExpensesUiState.Ready, currencyCode: String, onSettle: () -> Unit) {
    val isViewerPaying = payment.fromUserId == state.viewerId
    val other = state.members[if (isViewerPaying) payment.toUserId else payment.fromUserId]
    val otherName = other?.shortName ?: "A former housemate"
    ListRow(
        headline = if (isViewerPaying) "You owe $otherName" else "$otherName owes you",
        supporting = if (isViewerPaying) "Record it once you've paid" else "Record it once they've paid you",
        leading = { MemberAvatar(name = otherName, avatarUrl = other?.avatarUrl) },
        trailing = {
            TrailingAmount(
                amount = payment.amount.formatMoney(currencyCode),
                detail = if (isViewerPaying) "you pay" else "to you",
                detailColor = balanceColor(if (isViewerPaying) -payment.amount else payment.amount),
            )
        },
        onClick = onSettle,
    )
}
