/** A house's chores in three tabs: what's to do, what's done, and this month's leaderboard. */
package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.chores.presentation.ChoresUiState
import `in`.xroden.flockr.features.chores.presentation.ChoresViewModel
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

private val TABS = listOf("To do", "Done", "Leaderboard")

@Composable
fun ChoresScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onAddChore: () -> Unit,
    onEditChore: (choreId: String) -> Unit,
    viewModel: ChoresViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            haptics.error()
            snackbarHostState.showSnackbar(notice.message)
        }
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Chores", onNavigateBack = onNavigateBack) },
        floatingActionButton = { FlockrExtendedFab(text = "Add chore", icon = Icons.Rounded.Add, onClick = onAddChore) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                TABS.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { haptics.select(); selectedTab = index }, text = { Text(title) }, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(Modifier.fillMaxSize()) {
                when (val current = state) {
                    ChoresUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                    is ChoresUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                    is ChoresUiState.Ready -> when (selectedTab) {
                        0 -> ChoreList(
                            chores = current.open,
                            state = current,
                            config = config,
                            empty = { EmptyState(icon = Icons.Rounded.CleaningServices, title = "Nothing to do", subtitle = "Add a chore, and make it repeat to take turns automatically.", actionText = "Add a chore", onActionClick = onAddChore) },
                            onToggle = { chore, done -> viewModel.setCompleted(chore, done) },
                            onEdit = onEditChore,
                        )
                        1 -> ChoreList(
                            chores = current.done,
                            state = current,
                            config = config,
                            header = { if (current.done.isNotEmpty()) TextButton(onClick = { viewModel.clearDone(houseId) }) { Text("Clear done chores") } },
                            empty = { EmptyState(icon = Icons.Rounded.TaskAlt, title = "Nothing done yet", subtitle = "Ticked-off chores show up here.") },
                            onToggle = { chore, done -> viewModel.setCompleted(chore, done) },
                            onEdit = onEditChore,
                        )
                        else -> Leaderboard(current)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoreList(
    chores: List<Chore>,
    state: ChoresUiState.Ready,
    config: HouseConfig?,
    empty: @Composable () -> Unit,
    onToggle: (Chore, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    header: @Composable () -> Unit = {},
) {
    if (chores.isEmpty()) {
        empty()
        return
    }
    val (mine, others) = chores.partition { it.assignedTo == state.viewerId }
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
        item(key = "header") { header() }
        listOf("Yours" to mine, "Everyone else's" to others).forEach { (title, group) ->
            if (group.isEmpty()) return@forEach
            item(key = title) {
                Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Spacing.lg, top = Spacing.lg))
            }
            items(group, key = { it.id }) { chore ->
                ChoreRow(chore, state, config, onToggle = { onToggle(chore, it) }, onClick = { onEdit(chore.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ChoreRow(chore: Chore, state: ChoresUiState.Ready, config: HouseConfig?, onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Checkbox(checked = chore.isCompleted, onCheckedChange = { haptics.toggle(it); onToggle(it) })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(chore.taskName, style = MaterialTheme.typography.bodyLargeEmphasized)
            val due = dueLabel(chore, state.today, config)
            val assignee = chore.assignedTo?.let { state.members.nameOf(it, state.viewerId) } ?: "Anyone"
            Text(
                listOfNotNull(assignee, due, chore.recurrencePattern?.name?.lowercase()).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (!chore.isCompleted && chore.dueDate != null && chore.dueDate < state.today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (chore.rotation.size > 1) {
                Text(
                    "Turns: " + chore.rotation.joinToString(" → ") { state.members.nameOf(it, state.viewerId) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AssistChip(onClick = onClick, label = { Text("${chore.effortPoints} pt${if (chore.effortPoints == 1) "" else "s"}") })
    }
}

private fun dueLabel(chore: Chore, today: LocalDate, config: HouseConfig?): String? {
    if (chore.isCompleted) return chore.completedAt?.let { "done" }
    val due = chore.dueDate ?: return null
    return when (val days = today.daysUntil(due)) {
        in Int.MIN_VALUE..-1 -> "overdue since ${due.formatWithHouseConfig(config)}"
        0 -> "due today"
        1 -> "due tomorrow"
        in 2..6 -> "due in $days days"
        else -> "due ${due.formatWithHouseConfig(config)}"
    }
}

/** Effort points earned this month, so the heavy chores count for more than the quick ones. */
@Composable
private fun Leaderboard(state: ChoresUiState.Ready) {
    if (state.scores.isEmpty()) {
        EmptyState(icon = Icons.Rounded.EmojiEvents, title = "No points yet this month", subtitle = "Each chore done earns its effort points. The board resets every month.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        itemsIndexed(state.scores, key = { _, score -> score.userId }) { index, score ->
            val member = state.members[score.userId]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("${index + 1}", style = MaterialTheme.typography.titleLargeEmphasized, color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                MemberAvatar(name = member?.displayName ?: "?", avatarUrl = member?.avatarUrl)
                Column(Modifier.weight(1f)) {
                    Text(state.members.nameOf(score.userId, state.viewerId), style = MaterialTheme.typography.bodyLargeEmphasized)
                    Text("${score.done} chore${if (score.done == 1) "" else "s"} done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${score.points} pts", style = MaterialTheme.typography.titleMediumEmphasized)
            }
        }
    }
}
