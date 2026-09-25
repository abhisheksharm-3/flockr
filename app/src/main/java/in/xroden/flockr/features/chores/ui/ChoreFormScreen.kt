/** Adding or editing a chore, including whose turn it rotates through and how many points it's worth. */
package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.data.enums.ChoreRecurrence
import `in`.xroden.flockr.features.chores.presentation.ChoreFormViewModel
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

private const val MAX_EFFORT_POINTS = 10

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
        topBar = { FlockrTopAppBar(title = if (form.isEditing) "Edit chore" else "Add chore", onNavigateBack = onNavigateBack) },
        bottomBar = {
            FlockrPrimaryButton(
                text = if (form.isEditing) "Save changes" else "Add chore",
                onClick = { viewModel.save(houseId) },
                enabled = form.canSave,
                isLoading = form.isSaving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!form.isLoaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FormSectionCard(icon = Icons.Rounded.CleaningServices, title = "Chore") {
                FlockrTextField(
                    value = form.taskName,
                    onValueChange = { name -> viewModel.update { it.copy(taskName = name) } },
                    label = "What needs doing",
                    placeholder = "Take out the bins, clean the kitchen…",
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlockrTextField(
                    value = form.description,
                    onValueChange = { text -> viewModel.update { it.copy(description = text) } },
                    label = "Details",
                    singleLine = false,
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Effort: ${form.effortPoints} of $MAX_EFFORT_POINTS", style = MaterialTheme.typography.bodyLargeEmphasized)
                Slider(
                    value = form.effortPoints.toFloat(),
                    onValueChange = { value -> viewModel.update { it.copy(effortPoints = value.toInt()) } },
                    valueRange = 1f..MAX_EFFORT_POINTS.toFloat(),
                    steps = MAX_EFFORT_POINTS - 2,
                    enabled = isEnabled,
                )
                Text("Harder chores earn more points on the leaderboard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FormSectionCard(icon = Icons.Rounded.Event, title = "When") {
                ToggleRow(
                    title = "Has a due date",
                    checked = form.dueDate != null,
                    onCheckedChange = { on -> viewModel.update { it.copy(dueDate = if (on) it.today else null) } },
                    enabled = isEnabled,
                )
                form.dueDate?.let { date ->
                    DatePickerField(label = "Due", date = date, houseConfig = config, onDateChange = { due -> viewModel.update { it.copy(dueDate = due) } }, enabled = isEnabled)
                }
                ChoiceField(
                    label = "Repeats",
                    selected = form.recurrence,
                    options = listOf(null) + ChoreRecurrence.entries,
                    onSelect = { recurrence -> viewModel.update { it.copy(recurrence = recurrence) } },
                    optionLabel = { it?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Doesn't repeat" },
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FormSectionCard(icon = Icons.Rounded.Group, title = "Who") {
                ChoiceField(
                    label = "Assigned to",
                    selected = form.members.firstOrNull { it.userId == form.assignedTo },
                    options = listOf(null) + form.members,
                    onSelect = { member -> viewModel.update { it.copy(assignedTo = member?.userId) } },
                    optionLabel = { it?.displayName ?: "Anyone" },
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (form.recurrence != null) {
                    Text("Take turns", style = MaterialTheme.typography.bodyLargeEmphasized)
                    Text(
                        "Tick people in the order they take turns. Each time it's done, the next one is up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                        form.members.forEach { member ->
                            val turn = form.rotation.indexOf(member.userId)
                            FilterChip(
                                selected = turn >= 0,
                                onClick = { haptics.select(); viewModel.toggleRotation(member.userId) },
                                label = { Text(if (turn >= 0) "${turn + 1}. ${member.displayName}" else member.displayName) },
                                enabled = isEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}
