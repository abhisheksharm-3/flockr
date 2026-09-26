/**
 * Adding or editing a chore: its name typed large on cobalt, then who, when, how often, how much it
 * counts and who takes turns as a sentence of tappable words.
 */
package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.chores.model.ChoreRecurrence
import `in`.xroden.flockr.features.chores.presentation.ChoreFormState
import `in`.xroden.flockr.features.chores.presentation.ChoreFormViewModel
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceNote
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.inputs.FlockrDatePickerDialog
import `in`.xroden.flockr.ui.components.inputs.OptionSheet
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.relativeDayLabel
import `in`.xroden.flockr.utils.rememberHaptics

private const val MAX_EFFORT_POINTS = 10

/** The pickers a sentence token can open; one at a time. */
private enum class Picker { ASSIGNEE, DUE, DATE, RECURRENCE, POINTS, ROTATION }

/** What the due token offers: a date from the calendar, or none at all. */
private enum class DueChoice(val label: String) { PICK("Pick a date"), NONE("No due date") }

@Composable
fun ChoreFormScreen(
    houseId: String,
    choreId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ChoreFormViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    val isEnabled = !form.isSaving
    var picker by remember { mutableStateOf<Picker?>(null) }

    LaunchedEffect(houseId, choreId) { viewModel.initialize(houseId, choreId) }
    LaunchedEffect(Unit) {
        viewModel.saved.collect {
            haptics.success()
            onSaved()
        }
    }
    LaunchedEffect(form.error) {
        val message = form.error ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        bottomBar = {
            if (form.isLoaded) {
                FormSubmitBar(
                    text = if (form.isEditing) "Save changes" else "Add chore",
                    onClick = { viewModel.save(houseId) },
                    enabled = form.canSave,
                    isLoading = form.isSaving,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!form.isLoaded) {
            SkeletonFormScreen()
            return@Scaffold
        }
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            hero = {
                FormHero(if (form.isEditing) "Edit chore" else "New chore") {
                    HeroTextInput(
                        value = form.taskName,
                        onValueChange = { name -> viewModel.update { it.copy(taskName = name) } },
                        placeholder = "Take out the bins",
                        enabled = isEnabled,
                        autoFocus = !form.isEditing,
                    )
                    HeroNote(choreNote(form))
                }
            },
        ) {
            ChoreSentence(form, config, isEnabled, onPick = { picker = it })
            SentenceNote(
                value = form.description,
                onValueChange = { text -> viewModel.update { it.copy(description = text) } },
                placeholder = "Where the bin bags live, what counts as clean",
                enabled = isEnabled,
            )
        }
    }

    when (picker) {
        Picker.ASSIGNEE -> OptionSheet(
            options = listOf(null) + form.members,
            selected = form.members.firstOrNull { it.userId == form.assignedTo },
            onSelect = { member -> viewModel.update { it.copy(assignedTo = member?.userId) } },
            onDismiss = { picker = null },
            title = "Who's doing it?",
            optionLabel = { it?.displayName ?: "Anyone" },
        )
        Picker.DUE -> OptionSheet(
            options = DueChoice.entries,
            selected = DueChoice.NONE.takeIf { form.dueDate == null },
            onSelect = { choice ->
                if (choice == DueChoice.PICK) picker = Picker.DATE else viewModel.update { it.copy(dueDate = null) }
            },
            onDismiss = { if (picker == Picker.DUE) picker = null },
            title = "When is it due?",
            optionLabel = { it.label },
        )
        Picker.DATE -> (form.dueDate ?: form.today)?.let { date ->
            FlockrDatePickerDialog(
                initialDate = date,
                firstDayOfWeek = config?.firstDayOfWeek,
                onDateSelected = { due -> viewModel.update { it.copy(dueDate = due) }; picker = null },
                onDismiss = { picker = null },
            )
        }
        Picker.RECURRENCE -> OptionSheet(
            options = listOf(null) + ChoreRecurrence.entries,
            selected = form.recurrence,
            onSelect = { recurrence -> viewModel.update { it.copy(recurrence = recurrence) } },
            onDismiss = { picker = null },
            title = "How often?",
            optionLabel = { it?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Doesn't repeat" },
        )
        Picker.POINTS -> OptionSheet(
            options = (1..MAX_EFFORT_POINTS).toList(),
            selected = form.effortPoints,
            onSelect = { points -> viewModel.update { it.copy(effortPoints = points) } },
            onDismiss = { picker = null },
            title = "How much effort is it?",
            optionLabel = ::pointsLabel,
        )
        Picker.ROTATION -> ModalBottomSheet(onDismissRequest = { picker = null }) {
            Column(Modifier.navigationBarsPadding().padding(vertical = Spacing.sm)) {
                Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Who takes turns?", style = MaterialTheme.typography.titleLargeEmphasized)
                    Text(
                        "Tap people in the order they take turns. Each time it's done, the next one is up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                form.members.forEach { member ->
                    val turn = form.rotation.indexOf(member.userId)
                    ListRow(
                        headline = member.displayName,
                        supporting = if (turn >= 0) "Turn ${turn + 1}" else null,
                        leading = { MemberAvatar(name = member.displayName, avatarUrl = member.avatarUrl) },
                        trailing = if (turn >= 0) ({ Icon(Icons.Rounded.Check, contentDescription = "In the rotation", tint = MaterialTheme.colorScheme.primary) }) else null,
                        onClick = {
                            haptics.toggle(turn < 0)
                            viewModel.toggleRotation(member.userId)
                        }.takeIf { isEnabled },
                    )
                }
            }
        }
        null -> Unit
    }
}

/** "For **Riya**, due **Friday**, repeats **weekly**, worth **2 points**, taking turns between **3 people**." */
@Composable
private fun ChoreSentence(form: ChoreFormState, config: HouseConfig?, enabled: Boolean, onPick: (Picker) -> Unit) {
    Sentence {
        SentenceWords("For")
        SentenceToken(
            form.members.firstOrNull { it.userId == form.assignedTo }?.shortName ?: "anyone",
            onClick = { onPick(Picker.ASSIGNEE) },
            enabled = enabled,
            icon = Icons.Rounded.Person,
        )
        SentenceWords("due")
        SentenceToken(
            form.dueDate?.relativeDayLabel(config) ?: "whenever",
            onClick = { onPick(Picker.DUE) },
            enabled = enabled,
            icon = Icons.Rounded.CalendarMonth,
            isUnset = form.dueDate == null,
        )
        val recurrence = form.recurrence
        if (recurrence != null) {
            SentenceWords("repeats")
            SentenceToken(recurrence.name.lowercase(), onClick = { onPick(Picker.RECURRENCE) }, enabled = enabled, icon = Icons.Rounded.EventRepeat)
        } else {
            SentenceWords("and")
            SentenceToken("doesn't repeat", onClick = { onPick(Picker.RECURRENCE) }, enabled = enabled, icon = Icons.Rounded.EventRepeat, isUnset = true)
        }
        SentenceWords("worth")
        SentenceToken(pointsLabel(form.effortPoints), onClick = { onPick(Picker.POINTS) }, enabled = enabled, icon = Icons.Rounded.EmojiEvents)
        if (recurrence != null) {
            SentenceWords("taking turns between")
            SentenceToken(
                rotationLabel(form),
                onClick = { onPick(Picker.ROTATION) },
                enabled = enabled,
                icon = Icons.Rounded.Groups,
                isUnset = form.rotation.isEmpty(),
            )
        }
    }
}

private fun pointsLabel(points: Int): String = "$points point${if (points == 1) "" else "s"}"

/** "nobody yet", "just Riya", "Riya and Karan", or "3 people", in turn order. */
private fun rotationLabel(form: ChoreFormState): String {
    val names = form.rotation.map { id -> form.members.firstOrNull { it.userId == id }?.shortName ?: "someone" }
    return when (names.size) {
        0 -> "nobody yet"
        1 -> "just ${names.single()}"
        2 -> names.joinToString(" and ")
        else -> "${names.size} people"
    }
}

/** What the chore counts for, and who is up next once turns are set. */
private fun choreNote(form: ChoreFormState): String =
    if (form.recurrence != null && form.rotation.isNotEmpty()) "Each time it's done, the next person is up" else "Harder chores earn more points on the leaderboard"
