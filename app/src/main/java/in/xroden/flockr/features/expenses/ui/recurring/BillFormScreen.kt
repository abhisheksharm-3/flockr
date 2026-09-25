/** Adding or editing a recurring bill, with a preview of what each person owes per payment. */
package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.presentation.BillFormViewModel
import `in`.xroden.flockr.features.expenses.presentation.REMINDER_DAY_OPTIONS
import `in`.xroden.flockr.features.expenses.model.ExpenseCategories
import `in`.xroden.flockr.features.expenses.ui.SplitEditor
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

@Composable
fun BillFormScreen(
    houseId: String,
    billId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: BillFormViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    val isEnabled = !form.isSaving

    LaunchedEffect(houseId, billId) { viewModel.initialize(houseId, billId) }
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
        topBar = { FlockrTopAppBar(title = if (form.isEditing) "Edit bill" else "Add bill", onNavigateBack = onNavigateBack) },
        bottomBar = {
            FlockrPrimaryButton(
                text = if (form.isEditing) "Save changes" else "Add bill",
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
            FormSectionCard(icon = Icons.Rounded.Receipt, title = "Bill") {
                FlockrTextField(
                    value = form.name,
                    onValueChange = { name -> viewModel.update { it.copy(name = name) } },
                    label = "Name",
                    placeholder = "Rent, internet, electricity…",
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField(
                    value = form.amount,
                    onValueChange = { amount -> viewModel.update { it.copy(amount = amount) } },
                    currencyCode = form.currencyCode,
                    label = "Usual amount",
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoiceField(
                    label = "Category",
                    selected = form.category,
                    options = ExpenseCategories.DEFAULT,
                    onSelect = { category -> viewModel.update { it.copy(category = category) } },
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FormSectionCard(icon = Icons.Rounded.Event, title = "Schedule") {
                ChoiceField(
                    label = "Repeats",
                    selected = form.frequency,
                    options = ExpenseFrequency.entries,
                    onSelect = { frequency -> viewModel.update { it.copy(frequency = frequency) } },
                    optionLabel = { it.label },
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (form.frequency == ExpenseFrequency.CUSTOM) {
                    FlockrTextField(
                        value = form.customFrequencyDays,
                        onValueChange = { days -> viewModel.update { it.copy(customFrequencyDays = days) } },
                        label = "Every how many days",
                        keyboardType = KeyboardType.Number,
                        isError = form.customFrequencyDays.isNotEmpty() && form.parsedCustomDays == null,
                        supportingText = "From 1 to 366",
                        enabled = isEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                form.firstDueDate?.let { date ->
                    DatePickerField(
                        label = "First due",
                        date = date,
                        houseConfig = config,
                        onDateChange = { due -> viewModel.update { it.copy(firstDueDate = due) } },
                        enabled = isEnabled && !form.isEditing,
                    )
                }
                if (form.isEditing) {
                    Text(
                        "The schedule counts from the first due date, so it can't be changed once the bill exists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ToggleRow(
                    title = "Remind the house",
                    subtitle = "A notification at 9am on the reminder day",
                    checked = form.reminderEnabled,
                    onCheckedChange = { on -> viewModel.update { it.copy(reminderEnabled = on) } },
                    enabled = isEnabled,
                )
                if (form.reminderEnabled) {
                    ChoiceField(
                        label = "Remind",
                        selected = form.reminderDaysBefore,
                        options = REMINDER_DAY_OPTIONS,
                        onSelect = { days -> viewModel.update { it.copy(reminderDaysBefore = days) } },
                        optionLabel = { days -> if (days == 0) "On the day" else "$days day${if (days == 1) "" else "s"} before" },
                        enabled = isEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ToggleRow(
                    title = "Allow paying early",
                    subtitle = "Otherwise it can be paid from its reminder day on",
                    checked = form.allowPrepayment,
                    onCheckedChange = { on -> viewModel.update { it.copy(allowPrepayment = on) } },
                    enabled = isEnabled,
                )
            }
            FormSectionCard(icon = Icons.Rounded.Group, title = "Split") {
                SplitEditor(
                    draft = form.split,
                    members = form.members,
                    viewerId = form.viewerId,
                    currencyCode = form.currencyCode,
                    unassigned = form.unassigned,
                    owedByUser = form.preview.orEmpty().associate { it.userId to it.owedShare },
                    enabled = isEnabled,
                    offSubtitle = "Whoever pays bears it",
                    onEnabledChange = viewModel::onSplitEnabledChange,
                    onMethodChange = viewModel::onSplitMethodChange,
                    onParticipantChange = viewModel::onParticipantChange,
                    onValueChange = viewModel::onSplitValueChange,
                )
                if (form.split.isEnabled) {
                    Text(
                        "Each payment is split this way, scaled to what was actually paid that time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlockrTextField(
                value = form.notes,
                onValueChange = { notes -> viewModel.update { it.copy(notes = notes) } },
                label = "Notes",
                singleLine = false,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
