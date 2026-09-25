/** Recording a payment between you and a housemate, whichever way it went. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.SettleUpFormState
import `in`.xroden.flockr.features.expenses.presentation.SettleUpViewModel
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.rememberHaptics
import `in`.xroden.flockr.utils.toAmountInput
import java.math.BigDecimal

@Composable
fun SettleUpScreen(
    houseId: String,
    fromUserId: String?,
    toUserId: String?,
    amount: BigDecimal?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SettleUpViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(houseId) { viewModel.initialize(houseId, fromUserId, toUserId, amount) }
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
        topBar = { FlockrTopAppBar(title = "Record a payment", onNavigateBack = onNavigateBack) },
        bottomBar = {
            FlockrPrimaryButton(
                text = "Record payment",
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (form.members.isEmpty()) {
                Text("There's no one else in the house to settle with yet.", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            DirectionRow(form, onSwap = { haptics.select(); viewModel.onDirectionChange(!form.isViewerPaying) })
            ChoiceField(
                label = if (form.isViewerPaying) "You paid" else "Paid you",
                selected = form.members.firstOrNull { it.userId == form.otherUserId },
                options = form.members,
                onSelect = { member -> member?.let { viewModel.onOtherChange(it.userId) } },
                optionLabel = { it?.displayName ?: "Choose a housemate" },
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(
                value = form.amount,
                onValueChange = viewModel::onAmountChange,
                currencyCode = form.currencyCode,
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            SuggestedAmount(form, onUse = { viewModel.onAmountChange(it.toAmountInput(form.currencyCode)) })
            form.date?.let { date ->
                DatePickerField(label = "Date", date = date, houseConfig = config, onDateChange = viewModel::onDateChange, enabled = !form.isSaving)
            }
            FlockrTextField(
                value = form.note,
                onValueChange = viewModel::onNoteChange,
                label = "Note",
                placeholder = "Cash, UPI, bank transfer…",
                enabled = !form.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "You → Riya", with a button that flips it to "Riya → You". */
@Composable
private fun DirectionRow(form: SettleUpFormState, onSwap: () -> Unit) {
    val other = form.members.firstOrNull { it.userId == form.otherUserId }
    val otherName = other?.shortName ?: "Housemate"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
    ) {
        val you: @Composable () -> Unit = { Party("You", form.viewer?.displayName ?: "You", form.viewer?.avatarUrl) }
        val them: @Composable () -> Unit = { Party(otherName, otherName, other?.avatarUrl) }
        if (form.isViewerPaying) you() else them()
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "paid")
        if (form.isViewerPaying) them() else you()
        IconButton(onClick = onSwap) { Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap who paid") }
    }
}

@Composable
private fun Party(label: String, name: String, avatarUrl: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        MemberAvatar(name = name, avatarUrl = avatarUrl, size = ComponentHeight.avatarLarge)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Offers the amount that would settle the two exactly, when the plan has one and it isn't already entered. */
@Composable
private fun SuggestedAmount(form: SettleUpFormState, onUse: (BigDecimal) -> Unit) {
    val suggested = form.suggestedAmount ?: return
    if (form.parsedAmount?.compareTo(suggested) == 0) {
        Text("This settles you two exactly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        return
    }
    AssistChip(onClick = { onUse(suggested) }, label = { Text("Use ${suggested.formatMoney(form.currencyCode)}, the amount that settles you two") })
}
