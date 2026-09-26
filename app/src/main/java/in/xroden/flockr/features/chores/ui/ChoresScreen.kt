/** A house's chores: how many are on the viewer, then what's to do, what's done, and this month's leaderboard. */
package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroAmount
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.inputs.PillSelector
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.dueLabel
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics
import kotlinx.datetime.daysUntil

private val TABS = listOf("To do", "Done", "Leaderboard")
private const val RELATIVE_DUE_DAYS = 7

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
    var completions by remember { mutableIntStateOf(0) }
    val onToggle: (Chore, Boolean) -> Unit = { chore, done ->
        if (done) completions++
        viewModel.setCompleted(chore, done)
    }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            haptics.error()
            snackbarHostState.showSnackbar(notice.message)
        }
    }

    Scaffold(
        floatingActionButton = { FlockrExtendedFab(text = "Add chore", icon = Icons.Rounded.Add, onClick = onAddChore) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            ChoresUiState.Loading -> SkeletonHeroScreen()
            is ChoresUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) { ErrorState(current.message, onRetry = { viewModel.load(houseId) }) }
            is ChoresUiState.Ready -> Box(Modifier.fillMaxSize()) {
                val listState = rememberLazyListState()
                LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
                    item(key = "hero") { TurnHero(current, completions) }
                    item(key = "tabs") {
                        PillSelector(
                            tabs = TABS,
                            selectedIndex = selectedTab,
                            onTabSelected = { selectedTab = it },
                            counts = listOf(current.open.size, current.done.size),
                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        )
                    }
                    when (selectedTab) {
                        0 -> choreGroups(
                            chores = current.open,
                            state = current,
                            config = config,
                            empty = {
                                EmptyState(
                                    icon = Icons.Rounded.CleaningServices,
                                    title = "Nothing to do",
                                    subtitle = "Add a chore, and make it repeat to take turns automatically.",
                                    actionText = "Add a chore",
                                    onActionClick = onAddChore,
                                )
                            },
                            onToggle = onToggle,
                            onEdit = onEditChore,
                        )
                        1 -> choreGroups(
                            chores = current.done,
                            state = current,
                            config = config,
                            empty = { EmptyState(icon = Icons.Rounded.TaskAlt, title = "Nothing done yet", subtitle = "Ticked-off chores show up here.") },
                            onToggle = onToggle,
                            onEdit = onEditChore,
                            action = { TextButton(onClick = { viewModel.clearDone(houseId) }, shapes = ButtonDefaults.shapes()) { Text("Clear done chores") } },
                        )
                        else -> leaderboard(current)
                    }
                    item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
                }
                HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
            }
        }
    }
}

/**
 * The headline is how many open chores are the viewer's turn, with the overdue ones and this month's
 * leader under it. The tick pops each time [completions] goes up, since a ticked chore leaves the list.
 */
@Composable
private fun TurnHero(state: ChoresUiState.Ready, completions: Int) {
    val mine = state.open.filter { it.assignedTo == state.viewerId }
    val overdue = mine.count { chore -> chore.dueDate?.let { it < state.today } == true }
    val leader = state.scores.firstOrNull()?.let { score ->
        val leads = if (score.userId == state.viewerId) "lead" else "leads"
        "${state.members.nameOf(score.userId, state.viewerId)} $leads this month with ${pointsLabel(score.points)}."
    }
    HeroHeader(
        title = "Chores",
        actions = {
            AnimatedGlyph(
                Icons.Rounded.TaskAlt,
                trigger = completions,
                motion = GlyphMotion.POP,
                contentDescription = null,
                tint = MaterialTheme.flockrColors.onHeroVariant,
                modifier = Modifier.padding(end = Spacing.md),
            )
        },
    ) {
        if (mine.isEmpty()) {
            HeroLabel("You're all caught up")
            HeroCaption(leader ?: "Every chore done earns its effort points on the leaderboard.")
        } else {
            HeroLabel("Your turn on")
            HeroAmount(if (mine.size == 1) "1 chore" else "${mine.size} chores")
            HeroCaption(listOfNotNull(if (overdue > 0) "$overdue overdue." else null, leader).joinToString(" ").ifEmpty { "Tick one off to earn its points." })
        }
    }
}

/** The viewer's chores first, then everyone else's, each under its own heading; [action] sits on the first heading. */
private fun LazyListScope.choreGroups(
    chores: List<Chore>,
    state: ChoresUiState.Ready,
    config: HouseConfig?,
    empty: @Composable () -> Unit,
    onToggle: (Chore, Boolean) -> Unit,
    onEdit: (String) -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    if (chores.isEmpty()) {
        item(key = "empty") { empty() }
        return
    }
    val (mine, others) = chores.partition { it.assignedTo == state.viewerId }
    listOf("Yours" to mine, "Everyone else's" to others).filter { it.second.isNotEmpty() }.forEachIndexed { index, (title, group) ->
        item(key = "title_$title") { SectionTitle(title, action = action.takeIf { index == 0 }) }
        items(group, key = { it.id }) { chore ->
            ChoreRow(chore, state, config, onToggle = { onToggle(chore, it) }, onClick = { onEdit(chore.id) }, modifier = Modifier.animateItem())
        }
    }
}

/** A chore to tick off: who and how often underneath, its points and when it's due at the end, overdue in the error colour. */
@Composable
private fun ChoreRow(chore: Chore, state: ChoresUiState.Ready, config: HouseConfig?, onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val who = if (chore.isCompleted && chore.completedBy != null) {
        "Done by ${state.members.nameInSentence(chore.completedBy, state.viewerId)}"
    } else {
        chore.assignedTo?.let { state.members.nameOf(it, state.viewerId) } ?: "Anyone"
    }
    val repeats = chore.recurrencePattern?.let { "Repeats ${it.name.lowercase()}" }
    val turns = chore.rotation.takeIf { it.size > 1 }?.joinToString(" → ", prefix = "Turns: ") { state.members.nameOf(it, state.viewerId) }
    val due = chore.dueDate?.takeUnless { chore.isCompleted }?.let { date ->
        val days = state.today.daysUntil(date)
        if (days < RELATIVE_DUE_DAYS) dueLabel(days) else "Due ${date.formatWithHouseConfig(config)}"
    }
    val isOverdue = !chore.isCompleted && chore.dueDate?.let { it < state.today } == true
    ListRow(
        headline = chore.taskName,
        supporting = listOfNotNull(listOfNotNull(who, repeats).joinToString(" · "), turns).joinToString("\n"),
        leading = { Checkbox(checked = chore.isCompleted, onCheckedChange = { haptics.toggle(it); onToggle(it) }) },
        trailing = {
            TrailingAmount(
                amount = pointsLabel(chore.effortPoints),
                detail = due,
                detailColor = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
        modifier = modifier,
    )
}

/** Effort points earned this month, so the heavy chores count for more than the quick ones. */
private fun LazyListScope.leaderboard(state: ChoresUiState.Ready) {
    if (state.scores.isEmpty()) {
        item(key = "board_empty") {
            EmptyState(icon = Icons.Rounded.EmojiEvents, title = "No points yet this month", subtitle = "Each chore done earns its effort points. The board resets every month.")
        }
        return
    }
    item(key = "board_title") { SectionTitle("This month", subtitle = "The board resets on the 1st.") }
    itemsIndexed(state.scores, key = { _, score -> score.userId }) { index, score ->
        val member = state.members[score.userId]
        ListRow(
            headline = state.members.nameOf(score.userId, state.viewerId),
            supporting = "${score.done} chore${if (score.done == 1) "" else "s"} done",
            leading = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MemberAvatar(name = member?.displayName ?: "?", avatarUrl = member?.avatarUrl)
                }
            },
            trailing = { TrailingAmount(pointsLabel(score.points)) },
            modifier = Modifier.animateItem(),
        )
    }
}

private fun pointsLabel(points: Int): String = "$points pt${if (points == 1) "" else "s"}"
