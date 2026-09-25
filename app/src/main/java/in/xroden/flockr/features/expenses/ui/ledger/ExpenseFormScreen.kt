/** Adding or editing a one-time expense, with a live preview of exactly how it will be split. */
package `in`.xroden.flockr.features.expenses.ui.onetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.data.SplitShares
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormUiState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormViewModel
import `in`.xroden.flockr.features.expenses.ui.ExpenseCategories
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.inputs.PillSelector
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal

private val CustomShareFieldWidth = 128.dp

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
        ToggleRow(
            title = "Split this expense",
            subtitle = if (form.isSplitEnabled) "Shared with housemates" else "Only the payer bears it",
            checked = form.isSplitEnabled,
            onCheckedChange = viewModel::onSplitEnabledChange,
            enabled = !isSaving,
        )
        if (!form.isSplitEnabled) return@FormSectionCard

        PillSelector(
            tabs = listOf("Equally", "Custom amounts"),
            selectedIndex = if (form.isSplitEqual) 0 else 1,
            onTabSelected = { viewModel.onSplitEqualChange(it == 0) },
        )
        form.houseMembers.forEach { member ->
            key(member.userId) {
                MemberShareRow(
                    member = member,
                    isSelected = member.userId in form.selectedMemberIds,
                    customAmount = form.customSplits[member.userId].orEmpty(),
                    isCustom = !form.isSplitEqual,
                    currencyCode = form.currencyCode,
                    enabled = !isSaving,
                    onSelectedChange = { viewModel.onMemberSelectionChange(member.userId, it) },
                    onCustomAmountChange = { viewModel.onCustomSplitChange(member.userId, it) },
                )
            }
        }
        if (form.selectedMemberIds.isNotEmpty() && form.parsedAmount != null) {
            SplitPreview(plan = form.splitPlan, currencyCode = form.currencyCode, isViewerPayer = form.isViewerPayer)
        }
    }
}

@Composable
private fun MemberShareRow(
    member: MemberWithProfile,
    isSelected: Boolean,
    customAmount: String,
    isCustom: Boolean,
    currencyCode: String,
    enabled: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onCustomAmountChange: (String) -> Unit,
) {
    val haptics = rememberHaptics()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { haptics.toggle(it); onSelectedChange(it) },
            enabled = enabled,
        )
        Column(Modifier.weight(1f)) {
            Text(member.fullName ?: member.email, style = MaterialTheme.typography.bodyLargeEmphasized)
            if (member.fullName != null) {
                Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isCustom && isSelected) {
            AmountField(
                value = customAmount,
                onValueChange = onCustomAmountChange,
                currencyCode = currencyCode,
                label = null,
                enabled = enabled,
                modifier = Modifier.width(CustomShareFieldWidth),
            )
        }
    }
}

/**
 * What saving would record: the payer's own share and what the others owe them. When the custom
 * amounts do not add up it says so, rather than showing numbers that would not be saved.
 */
@Composable
private fun SplitPreview(plan: SplitShares?, currencyCode: String, isViewerPayer: Boolean) {
    if (plan == null) {
        Text(
            "Shares must not exceed the total, and the payer's must match what is left",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }
    val owedToPayer = plan.rows.values.fold(BigDecimal.ZERO, BigDecimal::add)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        PreviewLine(if (isViewerPayer) "Your share" else "Payer's share", plan.payerShare.formatMoney(currencyCode))
        PreviewLine(if (isViewerPayer) "Owed to you" else "Owed to the payer", owedToPayer.formatMoney(currencyCode))
    }
}

@Composable
private fun PreviewLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMediumEmphasized)
    }
}
