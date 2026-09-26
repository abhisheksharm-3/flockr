/** A month of usage: logging a use in one tap, the month as a calendar, who used what, billing it, the items and the log. */
package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import `in`.xroden.flockr.features.expenses.presentation.PerDiemUiState
import `in`.xroden.flockr.features.expenses.presentation.PerDiemViewModel
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.expenses.ui.reports.HeroMonthSelector
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonRow
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroCountUp
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceNote
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.FlockrDatePickerDialog
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.monthYearLabel
import `in`.xroden.flockr.utils.parseDecimal
import `in`.xroden.flockr.utils.relativeDayLabel
import `in`.xroden.flockr.utils.rememberHaptics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.flockrColors
import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

private const val SKELETON_ROWS = 4
private const val DAYS_IN_WEEK = 7
private const val MIN_DAY_STRENGTH = 0.25f
private const val HALF = 0.5f
private const val FUTURE_ALPHA = 0.4f
private const val PERCENT = 100

/** Where the usage screen leads. */
data class PerDiemNavigation(
    val back: () -> Unit,
    val logUsage: (configId: String?) -> Unit,
    val addItem: () -> Unit,
    val editItem: (configId: String) -> Unit,
    val openExpense: (expenseId: String) -> Unit,
)

@Composable
fun PerDiemScreen(houseId: String, navigation: PerDiemNavigation, viewModel: PerDiemViewModel = hiltViewModel()) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    var editingEntry by remember { mutableStateOf<PerDiemEntryWithDetails?>(null) }
    var isConfirmingBill by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { notice ->
            if (notice.isError) haptics.error() else haptics.success()
            snackbarHostState.showSnackbar(notice.message)
        }
    }

    Scaffold(
        floatingActionButton = { FlockrExtendedFab(text = "Log usage", icon = Icons.Rounded.Add, onClick = { navigation.logUsage(null) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
                item(key = "hero") {
                    UsageHero(
                        state = state,
                        month = month,
                        config = config,
                        navigation = navigation,
                        onMonthChange = viewModel::onMonthChange,
                        onBill = { isConfirmingBill = true },
                    )
                }
                when (val current = state) {
                    PerDiemUiState.Loading -> item(key = "loading") {
                        Column { repeat(SKELETON_ROWS) { SkeletonRow() } }
                    }
                    is PerDiemUiState.Error -> item(key = "error") { ErrorState(current.message, onRetry = { viewModel.load(houseId) }) }
                    is PerDiemUiState.Ready -> usageSections(
                        state = current,
                        month = month,
                        config = config,
                        navigation = navigation,
                        onEditEntry = { editingEntry = it },
                    )
                }
            }
            HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
        }
    }

    val ready = state as? PerDiemUiState.Ready
    if (isConfirmingBill && ready != null) {
        ConfirmDialog(
            title = "Bill ${month?.monthYearLabel().orEmpty()}?",
            message = "Record that you paid ${ready.data.total.formatMoney(config.currency())} for this month's usage. " +
                "Each person will owe you what they used, and the month's log will be locked.",
            confirmText = "Bill it",
            onConfirm = {
                isConfirmingBill = false
                viewModel.billMonth()
            },
            onDismiss = { isConfirmingBill = false },
        )
    }
    editingEntry?.let { entry ->
        EditEntrySheet(
            entry = entry,
            config = config,
            onSave = { quantity, date, notes ->
                editingEntry = null
                viewModel.updateEntry(entry, quantity, date, notes)
            },
            onDelete = {
                editingEntry = null
                viewModel.deleteEntry(entry)
            },
            onDismiss = { editingEntry = null },
        )
    }
}

/** The month's total on the cobalt, with the one thing to do about it: bill it, or open the bill it became. */
@Composable
private fun UsageHero(
    state: PerDiemUiState,
    month: LocalDate?,
    config: HouseConfig?,
    navigation: PerDiemNavigation,
    onMonthChange: (LocalDate) -> Unit,
    onBill: () -> Unit,
) {
    HeroHeader(title = "Usage") {
        month?.let { HeroMonthSelector(it, onMonthChange, config) }
        when (state) {
            is PerDiemUiState.Ready -> {
                val data = state.data
                val bill = data.usageBill
                HeroLabel("Used this month")
                HeroCountUp(data.total, config.currency())
                HeroCaption(
                    when {
                        bill != null -> "Billed, so this month's log is locked."
                        data.total.signum() > 0 -> "Bill it once the month is done, and everyone owes you what they used."
                        data.items.isEmpty() -> "Add what the house buys by use, like milk or water cans."
                        else -> "Nothing logged yet. Log what you use and it adds up here."
                    }
                )
                when {
                    bill != null -> HeroActions { HeroSecondaryButton("See the bill", onClick = { navigation.openExpense(bill.id) }) }
                    data.total.signum() > 0 -> HeroActions { HeroButton("Bill this month", onClick = onBill, enabled = !state.isBilling) }
                }
            }
            PerDiemUiState.Loading -> HeroLabel("Adding up the month")
            is PerDiemUiState.Error -> HeroLabel("This month didn't load")
        }
    }
}

/**
 * Below the hero, in the order a housemate reaches for them: log a use, see the month as a calendar,
 * see who used what, then the items and the log itself.
 */
private fun LazyListScope.usageSections(
    state: PerDiemUiState.Ready,
    month: LocalDate?,
    config: HouseConfig?,
    navigation: PerDiemNavigation,
    onEditEntry: (PerDiemEntryWithDetails) -> Unit,
) {
    val data = state.data
    val currencyCode = config.currency()
    val isLocked = data.usageBill != null
    if (data.items.isNotEmpty() && !isLocked) {
        item(key = "log_title") { SectionTitle("Log a use") }
        item(key = "quick_log") { QuickLog(data.items, data.entries, onLog = navigation.logUsage, onAddItem = navigation.addItem) }
    }
    if (month != null && data.entries.isNotEmpty()) {
        item(key = "calendar_title") { SectionTitle("This month", subtitle = "Darker days used more. Tap one to see it.") }
        item(key = "calendar") { UsageCalendar(month, data.entries, config, currencyCode) }
    }
    if (data.byMember.isNotEmpty()) {
        item(key = "members_title") { SectionTitle("Who used what") }
        items(data.byMember, key = { "member_${it.userId}" }) { member ->
            MemberShareRow(member, isViewer = member.userId == state.viewerId, total = data.total, currencyCode = currencyCode)
        }
    }
    item(key = "items_title") {
        SectionTitle("Items", action = { TextButton(onClick = navigation.addItem, shapes = ButtonDefaults.shapes()) { Text("Add item") } })
    }
    if (data.items.isEmpty()) {
        item(key = "items_empty") { QuietLine("Nothing to log yet. Add what the house buys by use, like milk or water cans, with its price per unit.") }
    }
    items(data.items, key = { "item_${it.id}" }) { item ->
        val used = data.entries.filter { it.configId == item.id }
        ItemRow(item, used, currencyCode, onEdit = { navigation.editItem(item.id) })
    }
    if (data.entries.isEmpty() && data.items.isNotEmpty()) {
        item(key = "entries_title") { SectionTitle("Log") }
        item(key = "entries_empty") { QuietLine("Nothing logged in ${month?.monthYearLabel() ?: "this month"} yet. Tap an item above to log a use.") }
    }
    data.entries.groupBy { it.date }.toSortedMap(compareByDescending { it }).forEach { (date, entries) ->
        item(key = "day_$date") {
            SectionTitle(date.relativeDayLabel(config), subtitle = if (isLocked) "Locked, since the month is billed" else null)
        }
        items(entries, key = { "entry_${it.id}" }) { entry ->
            EntryRow(entry, state.viewerId, config, onClick = if (isLocked) null else ({ onEditEntry(entry) }))
        }
    }
}

/**
 * One pill per item, so logging a use is a single tap from here. Each pill's plus pops when a use of
 * that item lands in the log.
 */
@Composable
private fun QuickLog(items: List<PerDiemConfig>, entries: List<PerDiemEntryWithDetails>, onLog: (String?) -> Unit, onAddItem: () -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items.forEach { item ->
            FilledTonalButton(
                onClick = { haptics.tap(); onLog(item.id) },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) {
                AnimatedGlyph(Icons.Rounded.Add, trigger = entries.count { it.configId == item.id }, motion = GlyphMotion.POP, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(item.itemName, style = MaterialTheme.typography.labelLargeEmphasized)
            }
        }
        OutlinedButton(onClick = onAddItem, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight)) {
            Text("New item", style = MaterialTheme.typography.labelLargeEmphasized)
        }
    }
}

/**
 * The month as a calendar laid out in the house's week, each day a circle shaded by what was used
 * that day against the busiest day. Tapping a day says what it came to. Days after today are faint.
 */
@Composable
private fun UsageCalendar(month: LocalDate, entries: List<PerDiemEntryWithDetails>, config: HouseConfig?, currencyCode: String) {
    val haptics = rememberHaptics()
    val today = config.today()
    val byDay = remember(entries) { entries.groupBy { it.date.day }.mapValues { (_, uses) -> uses.fold(BigDecimal.ZERO) { sum, use -> sum + use.totalCost } } }
    val busiest = byDay.values.maxOrNull()?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    val daysInMonth = month.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    val firstDay = config?.firstDayOfWeek ?: 1
    val lead = (month.dayOfWeek.isoDayNumber % DAYS_IN_WEEK - firstDay + DAYS_IN_WEEK) % DAYS_IN_WEEK
    var selected by remember(month) { mutableStateOf<Int?>(null) }
    val cells = List(lead) { null } + (1..daysInMonth).toList()
    Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (week + List(DAYS_IN_WEEK - week.size) { null }).forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(Spacing.xxs), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val date = LocalDate(month.year, month.month, day)
                            val cost = byDay[day]
                            DayDot(
                                day = day,
                                strength = cost?.let { (it.toFloat() / busiest.toFloat()).coerceIn(MIN_DAY_STRENGTH, 1f) } ?: 0f,
                                isToday = date == today,
                                isFuture = date > today,
                                isSelected = selected == day,
                                onClick = { haptics.select(); selected = if (selected == day) null else day },
                            )
                        }
                    }
                }
            }
        }
        selected?.let { day ->
            val uses = entries.filter { it.date.day == day }
            Text(
                if (uses.isEmpty()) "${LocalDate(month.year, month.month, day).relativeDayLabel(config).replaceFirstChar { it.uppercase() }}: nothing used"
                else "${LocalDate(month.year, month.month, day).relativeDayLabel(config).replaceFirstChar { it.uppercase() }}: " +
                    uses.joinToString { "${it.quantity.stripTrailingZeros().toPlainString()} ${it.unit} ${it.itemName}" } +
                    " · " + uses.fold(BigDecimal.ZERO) { sum, use -> sum + use.totalCost }.formatMoney(currencyCode),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun DayDot(day: Int, strength: Float, isToday: Boolean, isFuture: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val fill = if (strength > 0f) colors.primary.copy(alpha = strength) else colors.surfaceContainerHigh
    val ink = if (strength > HALF) colors.onPrimary else colors.onSurface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = fill,
        contentColor = ink,
        border = when {
            isSelected -> BorderStroke(Spacing.xxs, MaterialTheme.flockrColors.sun)
            isToday -> BorderStroke(Spacing.xxs, colors.primary)
            else -> null
        },
        modifier = Modifier.fillMaxSize().alpha(if (isFuture) FUTURE_ALPHA else 1f),
    ) {
        Box(contentAlignment = Alignment.Center) { Text("$day", style = MaterialTheme.typography.labelMedium) }
    }
}

/** A person's month: what they used, and a wavy bar of their part of the house's total. */
@Composable
private fun MemberShareRow(member: PerDiemBillByMember, isViewer: Boolean, total: BigDecimal, currencyCode: String) {
    val share = if (total.signum() > 0) (member.totalCost.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth()) {
        ListRow(
            headline = if (isViewer) "You" else member.fullName,
            supporting = "${(share * PERCENT).roundToInt()}% of the month",
            leading = { MemberAvatar(name = member.fullName, avatarUrl = null) },
            trailing = { TrailingAmount(member.totalCost.formatMoney(currencyCode)) },
        )
        LinearWavyProgressIndicator(
            progress = { share },
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.lg + ComponentHeight.avatar + Spacing.md, end = Spacing.lg, bottom = Spacing.sm),
        )
    }
}

/** An item, its price, and what it came to this month; tapping it edits the item. */
@Composable
private fun ItemRow(item: PerDiemConfig, used: List<PerDiemEntryWithDetails>, currencyCode: String, onEdit: () -> Unit) {
    val quantity = used.fold(BigDecimal.ZERO) { sum, use -> sum + use.quantity }
    val cost = used.fold(BigDecimal.ZERO) { sum, use -> sum + use.totalCost }
    ListRow(
        headline = item.itemName,
        supporting = "${item.rate.formatMoney(currencyCode)} per ${item.unit}" +
            if (used.isEmpty()) " · none this month" else " · ${quantity.stripTrailingZeros().toPlainString()} ${item.unit} this month",
        leading = { IconBadge(categoryIcon(item.category), BadgeTone.JADE) },
        trailing = { if (used.isNotEmpty()) TrailingAmount(cost.formatMoney(currencyCode)) },
        onClick = onEdit,
    )
}

@Composable
private fun EntryRow(entry: PerDiemEntryWithDetails, viewerId: String, config: HouseConfig?, onClick: (() -> Unit)?) {
    ListRow(
        headline = "${entry.quantity.stripTrailingZeros().toPlainString()} ${entry.unit} ${entry.itemName}",
        supporting = if (entry.addedBy == viewerId) "You" else entry.addedByName,
        leading = { MemberAvatar(name = entry.addedByName, avatarUrl = null) },
        trailing = { TrailingAmount(entry.totalCost.formatMoney(config.currency())) },
        onClick = onClick,
    )
}

@Composable
private fun QuietLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

/**
 * Changing or deleting a logged use, as a sheet: the quantity typed in full, then "Used on **today**"
 * and an optional note. The price stays what it was when the use was logged.
 */
@Composable
private fun EditEntrySheet(
    entry: PerDiemEntryWithDetails,
    config: HouseConfig?,
    onSave: (BigDecimal, LocalDate, String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var quantityText by rememberSaveable { mutableStateOf(entry.quantity.stripTrailingZeros().toPlainString()) }
    var date by remember { mutableStateOf(entry.date) }
    var notes by rememberSaveable { mutableStateOf(entry.notes.orEmpty()) }
    var isPickingDate by remember { mutableStateOf(false) }
    val quantity = parseDecimal(quantityText)?.takeIf { it.signum() > 0 }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text(entry.itemName, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = Spacing.lg))
            FlockrTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = "How much",
                suffix = entry.unit,
                keyboardType = KeyboardType.Decimal,
                isError = quantityText.isNotEmpty() && quantity == null,
                supportingText = if (quantityText.isNotEmpty() && quantity == null) "Type a number above zero" else null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            )
            Sentence {
                SentenceWords("Used on")
                SentenceToken(date.relativeDayLabel(config), onClick = { isPickingDate = true }, icon = Icons.Rounded.CalendarMonth)
            }
            SentenceNote(notes, { notes = it }, placeholder = "Anything the house should know")
            FormSubmitBar(
                text = "Save changes",
                onClick = { quantity?.let { onSave(it, date, notes.ifBlank { null }) } },
                enabled = quantity != null,
                isLoading = false,
                secondary = "Delete" to onDelete,
            )
        }
    }
    if (isPickingDate) {
        FlockrDatePickerDialog(
            initialDate = date,
            firstDayOfWeek = config?.firstDayOfWeek,
            onDateSelected = { date = it; isPickingDate = false },
            onDismiss = { isPickingDate = false },
        )
    }
}
