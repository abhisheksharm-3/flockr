/** A month of house spending at a glance: the total and its parts, by category, by person, and usage by item. */
package `in`.xroden.flockr.features.expenses.ui.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.features.expenses.presentation.ReportsUiState
import `in`.xroden.flockr.features.expenses.presentation.ReportsViewModel
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonRow
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroCountUp
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.Section
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.balanceColor
import `in`.xroden.flockr.ui.components.charts.ChartEntry
import `in`.xroden.flockr.ui.components.charts.SimplePieChart
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Motion
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.ui.theme.spatialSpec
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.monthYearLabel
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private const val SKELETON_ROWS = 4

private const val HERO_TONAL_ALPHA = 0.16f
private const val DISABLED_ALPHA = 0.38f

@Composable
fun ReportsScreen(houseId: String, onNavigateBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val currencyCode = config.currency()
    val listState = rememberLazyListState()

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                item(key = "hero") {
                    HeroHeader(title = "Reports") {
                        month?.let { HeroMonthSelector(it, viewModel::onMonthChange, config) }
                        when (val current = state) {
                            is ReportsUiState.Ready -> {
                                HeroLabel("The house spent")
                                HeroCountUp(current.summary.totalSpend, currencyCode)
                                HeroCaption("Not counting payments between housemates.")
                            }
                            ReportsUiState.Loading -> HeroLabel("Adding up the month")
                            is ReportsUiState.Error -> HeroLabel("This month didn't load")
                        }
                    }
                }
                when (val current = state) {
                    ReportsUiState.Loading -> item(key = "loading") {
                        Column { repeat(SKELETON_ROWS) { SkeletonRow() } }
                    }
                    is ReportsUiState.Error -> item(key = "error") { ErrorState(current.message, onRetry = { viewModel.load(houseId) }) }
                    is ReportsUiState.Ready -> reportSections(current, currencyCode)
                }
                item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
            }
            HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
        }
    }
}

private fun LazyListScope.reportSections(state: ReportsUiState.Ready, currencyCode: String) {
    val summary = state.summary
    item(key = "parts_title") { SectionTitle("Where it went") }
    item(key = "one_off") { PartRow("One-off expenses", Icons.AutoMirrored.Rounded.ReceiptLong, BadgeTone.COBALT, summary.oneTimeSpend, currencyCode) }
    item(key = "bills") { PartRow("Bills", Icons.Rounded.EventRepeat, BadgeTone.SUN, summary.recurringSpend, currencyCode) }
    item(key = "usage") { PartRow("Usage", Icons.Rounded.WaterDrop, BadgeTone.JADE, summary.perDiemSpend, currencyCode) }
    if (state.byCategory.isNotEmpty()) {
        item(key = "categories") {
            Section("By category") {
                SimplePieChart(
                    data = state.byCategory.map { ChartEntry(it.category, it.category, it.total) },
                    currencyCode = currencyCode,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
        }
    }
    if (state.byMember.isNotEmpty()) {
        item(key = "members_title") { SectionTitle("Who paid", subtitle = "What each person paid for the house, against their own share") }
        items(state.byMember.sortedByDescending { it.paid }, key = { "member_${it.userId}" }) { member -> MemberSpendRow(member, currencyCode) }
    }
    if (state.usage.isNotEmpty()) {
        item(key = "usage_title") { SectionTitle("Usage by item") }
        items(state.usage, key = { "item_${it.configId}" }) { item ->
            ListRow(
                headline = item.itemName,
                supporting = "${item.totalQuantity.stripTrailingZeros().toPlainString()} ${item.unit}",
                leading = { IconBadge(categoryIcon(item.category), BadgeTone.SLATE) },
                trailing = { TrailingAmount(item.totalCost.formatMoney(currencyCode)) },
            )
        }
    }
}

@Composable
private fun PartRow(title: String, icon: ImageVector, tone: BadgeTone, amount: BigDecimal, currencyCode: String) {
    ListRow(
        headline = title,
        leading = { IconBadge(icon, tone) },
        trailing = { TrailingAmount(amount.formatMoney(currencyCode)) },
    )
}

/** One person's month: what they paid, their share, and whether that leaves them ahead or behind, in a sign and a word. */
@Composable
private fun MemberSpendRow(member: SpendByMember, currencyCode: String) {
    val difference = member.paid - member.consumed
    ListRow(
        headline = member.fullName,
        supporting = "Paid ${member.paid.formatMoney(currencyCode)} · share ${member.consumed.formatMoney(currencyCode)}",
        leading = { MemberAvatar(name = member.fullName, avatarUrl = null) },
        trailing = {
            when (difference.signum()) {
                1 -> TrailingAmount("+${difference.formatMoney(currencyCode)}", "ahead", balanceColor(difference))
                -1 -> TrailingAmount("−${difference.abs().formatMoney(currencyCode)}", "behind", balanceColor(difference))
                else -> TrailingAmount("Even")
            }
        },
    )
}

/**
 * The month a hero is showing, centred between two translucent steppers that morph under the finger.
 * The label slides the way the month moved. Stepping stops at the house's current month.
 */
@Composable
internal fun HeroMonthSelector(selectedMonth: LocalDate, onMonthChange: (LocalDate) -> Unit, config: HouseConfig?) {
    val haptics = rememberHaptics()
    val colors = MaterialTheme.flockrColors
    val thisMonth = remember(config) { config.today().let { LocalDate(it.year, it.month, 1) } }
    val canGoForward = selectedMonth.plus(1, DateTimeUnit.MONTH) <= thisMonth
    val stepperColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = colors.onHero.copy(alpha = HERO_TONAL_ALPHA),
        contentColor = colors.onHero,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = colors.onHeroVariant.copy(alpha = DISABLED_ALPHA),
    )
    val slide = spatialSpec<IntOffset>()
    val fade = Motion.effects
    val step: (Int) -> Unit = { months ->
        haptics.select()
        onMonthChange(selectedMonth.plus(months, DateTimeUnit.MONTH))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = { step(-1) }, shapes = IconButtonDefaults.shapes(), colors = stepperColors) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
        }
        AnimatedContent(
            targetState = selectedMonth,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(slide) { it / 2 * direction } + fadeIn(fade)) togetherWith
                    (slideOutHorizontally(slide) { -it / 2 * direction } + fadeOut(fade))
            },
            contentAlignment = Alignment.Center,
            label = "month",
        ) { month ->
            Text(month.monthYearLabel(), style = MaterialTheme.typography.titleMediumEmphasized, color = colors.onHero, maxLines = 1)
        }
        FilledTonalIconButton(onClick = { step(1) }, enabled = canGoForward, shapes = IconButtonDefaults.shapes(), colors = stepperColors) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
        }
    }
}
