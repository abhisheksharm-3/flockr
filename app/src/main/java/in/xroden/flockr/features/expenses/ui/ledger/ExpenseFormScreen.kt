/** Adding or editing an expense, with a live preview of exactly what each person will owe. */
package `in`.xroden.flockr.features.expenses.ui.ledger

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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormUiState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormViewModel
import `in`.xroden.flockr.features.expenses.model.ExpenseCategories
import `in`.xroden.flockr.features.expenses.ui.SplitEditor
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics


/**
 * The expense form. With [expenseId] it edits that expense; without, it adds a new one, optionally
 * prefilled from a shopping item via [initialName] and [initialQuantity]. [onSaved] runs only once
 * the expense has actually been written, so a failed save keeps the user on the form with the error.
 */
@Composable
fun ExpenseFormScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    expenseId: String? = null,
    initialName: String? = null,
    initialQuantity: Int? = null,
    viewModel: ExpenseFormViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val houseConfig by viewModel.houseConfig.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isSaving = uiState is ExpenseFormUiState.Saving

    LaunchedEffect(houseId, expenseId) { viewModel.initialize(houseId, expenseId, initialName, initialQuantity) }
    LaunchedEffect(Unit) {
        viewModel.saved.collect {
            haptics.success()
            onSaved()
        }
    }
    LaunchedEffect(uiState) {
        val error = uiState as? ExpenseFormUiState.Error ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(error.message)
        viewModel.dismissError()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlockrTopAppBar(
                title = if (form.isEditing) "Edit expense" else "Add expense",
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            FlockrPrimaryButton(
                text = if (form.isEditing) "Save changes" else "Add expense",
                onClick = { viewModel.save(houseId) },
                enabled = form.canSave,
                isLoading = isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!form.isLoaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            DetailsSection(form, houseConfig, isSaving, viewModel)
            SplitSection(form, isSaving, viewModel)
        }
    }
}

@Composable
private fun DetailsSection(form: ExpenseFormState, houseConfig: HouseConfig?, isSaving: Boolean, viewModel: ExpenseFormViewModel) {
    FormSectionCard(icon = Icons.Filled.Receipt, title = "Details") {
        FlockrTextField(
            value = form.name,
            onValueChange = viewModel::onNameChange,
            label = "What was it for?",
            placeholder = "Groceries, electricity bill…",
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        AmountField(
            value = form.amount,
            onValueChange = viewModel::onAmountChange,
            currencyCode = form.currencyCode,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        form.date?.let { date ->
            DatePickerField(label = "Date", date = date, houseConfig = houseConfig, onDateChange = viewModel::onDateChange, enabled = !isSaving)
        }
        ChoiceField(
            label = "Category",
            selected = form.category,
            options = ExpenseCategories.DEFAULT,
            onSelect = viewModel::onCategoryChange,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        FlockrTextField(
            value = form.notes,
            onValueChange = viewModel::onNotesChange,
            label = "Notes",
            singleLine = false,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SplitSection(form: ExpenseFormState, isSaving: Boolean, viewModel: ExpenseFormViewModel) {
    FormSectionCard(icon = Icons.Filled.Group, title = "Split") {
        ChoiceField(
            label = "Paid by",
            selected = form.houseMembers.firstOrNull { it.userId == form.payerId },
            options = form.houseMembers.filter { it.isActive || it.userId == form.payerId },
            onSelect = { member -> member?.let { viewModel.onPayerChange(it.userId) } },
            optionLabel = { member -> member?.let { if (it.userId == form.viewerId) "You" else it.displayName }.orEmpty() },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        SplitEditor(
            draft = form.split,
            members = form.houseMembers,
            viewerId = form.viewerId,
            currencyCode = form.currencyCode,
            unassigned = form.unassigned,
            owedByUser = form.shares.orEmpty().associate { it.userId to it.owedShare },
            enabled = !isSaving,
            offSubtitle = "Only the payer bears it",
            onEnabledChange = viewModel::onSplitEnabledChange,
            onMethodChange = viewModel::onSplitMethodChange,
            onParticipantChange = viewModel::onParticipantChange,
            onValueChange = viewModel::onSplitValueChange,
        )
    }
}
