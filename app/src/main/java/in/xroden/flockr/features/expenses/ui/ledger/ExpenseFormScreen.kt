/**
 * Adding or editing an expense: the amount typed large on cobalt, then the rest as a sentence of
 * tappable words, with a live line of exactly what each person will owe.
 */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CallSplit
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
import `in`.xroden.flockr.features.expenses.model.SplitMethod
import `in`.xroden.flockr.features.expenses.model.ExpenseCategories
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormUiState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseFormViewModel
import `in`.xroden.flockr.features.expenses.ui.SplitEditor
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroAmountInput
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceNote
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.inputs.FlockrDatePickerDialog
import `in`.xroden.flockr.ui.components.inputs.OptionSheet
import `in`.xroden.flockr.ui.components.inputs.amountHint
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.relativeDayLabel
import `in`.xroden.flockr.utils.rememberHaptics

/** The pickers a sentence token can open; one at a time. */
private enum class Picker { PAYER, DATE, CATEGORY, SPLIT }

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
    val isSaving = uiState is ExpenseFormUiState.Saving
    var picker by remember { mutableStateOf<Picker?>(null) }
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onReceiptPicked)
    }

    LaunchedEffect(houseId, expenseId) { viewModel.initialize(houseId, expenseId, initialName, initialQuantity) }
    LaunchedEffect(Unit) {
        viewModel.saved.collect {
            haptics.success()
            onSaved()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.savedAndReset.collect {
            haptics.success()
            snackbarHostState.showSnackbar("Expense added. Ready for the next one.")
        }
    }
    LaunchedEffect(uiState) {
        val error = uiState as? ExpenseFormUiState.Error ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(error.message)
        viewModel.dismissError()
    }

    Scaffold(
        bottomBar = {
            if (form.isLoaded) {
                FormSubmitBar(
                    text = if (form.isEditing) "Save changes" else "Add expense",
                    onClick = { viewModel.save(houseId) },
                    enabled = form.canSave,
                    isLoading = isSaving,
                    secondary = if (form.isEditing) null else "Add another" to { viewModel.save(houseId, startAnother = true) },
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
            hero = { FormHero(if (form.isEditing) "Edit expense" else "New expense") {
                HeroAmountInput(form.amount, viewModel::onAmountChange, form.currencyCode, enabled = !isSaving, autoFocus = !form.isEditing)
                val hint = amountHint(form.amount, form.currencyCode)
                HeroNote(hint ?: shareNote(form), isError = hint != null)
                HeroTextInput(
                    value = form.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "What was it for?",
                    enabled = !isSaving,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            } },
        ) {
            ExpenseSentence(form, houseConfig, isSaving, onPick = { picker = it })
            SentenceNote(form.notes, viewModel::onNotesChange, placeholder = "A receipt number, or what was in the bag", enabled = !isSaving)
            Sentence {
                SentenceToken(
                    text = when {
                        form.receipt != null -> "Receipt photo added"
                        form.hasSavedReceipt -> "Replace the receipt photo"
                        else -> "Add a receipt photo"
                    },
                    onClick = { receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !isSaving,
                    icon = Icons.Rounded.AddAPhoto,
                    isUnset = form.receipt == null && !form.hasSavedReceipt,
                )
            }
            WhoOwesWhat(form)
        }
    }

    when (picker) {
        Picker.PAYER -> OptionSheet(
            options = form.houseMembers.filter { it.isActive || it.userId == form.payerId },
            selected = form.houseMembers.firstOrNull { it.userId == form.payerId },
            onSelect = { viewModel.onPayerChange(it.userId) },
            onDismiss = { picker = null },
            title = "Who paid?",
            optionLabel = { if (it.userId == form.viewerId) "You" else it.displayName },
        )
        Picker.CATEGORY -> OptionSheet(
            options = ExpenseCategories.DEFAULT,
            selected = form.category,
            onSelect = viewModel::onCategoryChange,
            onDismiss = { picker = null },
            title = "What kind of spending?",
            icon = ::categoryIcon,
        )
        Picker.DATE -> form.date?.let { date ->
            FlockrDatePickerDialog(
                initialDate = date,
                firstDayOfWeek = houseConfig?.firstDayOfWeek,
                onDateSelected = { viewModel.onDateChange(it); picker = null },
                onDismiss = { picker = null },
            )
        }
        Picker.SPLIT -> ModalBottomSheet(onDismissRequest = { picker = null }) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("How is it shared?", style = MaterialTheme.typography.titleLargeEmphasized)
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
        null -> Unit
    }
}

/** "Paid by **you** on **today**, for **Groceries**, split **equally** between **everyone**." */
@Composable
private fun ExpenseSentence(form: ExpenseFormState, config: HouseConfig?, isSaving: Boolean, onPick: (Picker) -> Unit) {
    val enabled = !isSaving
    Sentence {
        SentenceWords("Paid by")
        SentenceToken(nameFor(form.payerId, form).lowercaseIfYou(), onClick = { onPick(Picker.PAYER) }, enabled = enabled)
        SentenceWords("on")
        form.date?.let { SentenceToken(it.relativeDayLabel(config), onClick = { onPick(Picker.DATE) }, enabled = enabled, icon = Icons.Rounded.CalendarMonth) }
        SentenceWords("for")
        SentenceToken(form.category, onClick = { onPick(Picker.CATEGORY) }, enabled = enabled, icon = categoryIcon(form.category))
        if (form.split.isEnabled) {
            SentenceWords("split")
            SentenceToken(splitLabel(form), onClick = { onPick(Picker.SPLIT) }, enabled = enabled, icon = Icons.Rounded.CallSplit)
        } else {
            SentenceWords("and")
            SentenceToken("not split", onClick = { onPick(Picker.SPLIT) }, enabled = enabled, icon = Icons.Rounded.CallSplit, isUnset = true)
        }
    }
}

/** "equally between everyone", "by shares between Riya and you", and so on. */
private fun splitLabel(form: ExpenseFormState): String {
    val active = form.houseMembers.filter { it.isActive }.map { it.userId }.toSet()
    val ids = form.split.participantIds
    val who = when {
        ids.isNotEmpty() && ids == active -> "everyone"
        ids.size == 1 -> "just ${nameFor(ids.single(), form).lowercaseIfYou()}"
        ids.size == 2 -> ids.joinToString(" and ") { nameFor(it, form).lowercaseIfYou() }
        else -> "${ids.size} people"
    }
    return "${form.split.method.phrase} between $who"
}

private fun String.lowercaseIfYou() = if (this == "You") "you" else this

private fun nameFor(userId: String, form: ExpenseFormState): String =
    if (userId == form.viewerId) "You" else form.houseMembers.firstOrNull { it.userId == userId }?.shortName ?: "Someone"

/** What the amount means for the viewer under the current split, or a nudge while it's blank. */
private fun shareNote(form: ExpenseFormState): String {
    val total = form.parsedAmount ?: return "Type what you spent"
    if (!form.split.isEnabled) return "${nameFor(form.payerId, form)} ${if (form.payerId == form.viewerId) "pay" else "pays"} all of it"
    val mine = form.shares?.firstOrNull { it.userId == form.viewerId }?.owedShare ?: return "The split doesn't add up to ${total.formatMoney(form.currencyCode)} yet"
    return if (mine.signum() == 0) "You're not in this split" else "Your share is ${mine.formatMoney(form.currencyCode)}"
}

/** Each person's share, exactly as it will be saved, once the amount and split add up. */
@Composable
private fun WhoOwesWhat(form: ExpenseFormState) {
    val shares = form.shares?.takeIf { form.split.isEnabled && it.isNotEmpty() } ?: return
    val byId = form.houseMembers.associateBy { it.userId }
    Column {
        SectionTitle("Who owes what")
        shares.sortedByDescending { it.owedShare }.forEach { share ->
            val member: MemberWithProfile? = byId[share.userId]
            ListRow(
                headline = nameFor(share.userId, form),
                supporting = if (share.userId == form.payerId) "paid ${share.paidShare.formatMoney(form.currencyCode)}" else null,
                leading = { MemberAvatar(name = member?.displayName ?: "?", avatarUrl = member?.avatarUrl) },
                trailing = { TrailingAmount(share.owedShare.formatMoney(form.currencyCode), if (form.split.method == SplitMethod.EQUAL) "equal share" else null) },
            )
        }
    }
}
