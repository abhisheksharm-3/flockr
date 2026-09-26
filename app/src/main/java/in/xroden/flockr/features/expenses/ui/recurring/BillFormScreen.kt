/**
 * Adding or editing a recurring bill: the usual amount typed large on cobalt with its name, then the
 * schedule, split and reminder as a sentence of tappable words.
 */
package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Payments
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.model.ExpenseCategories
import `in`.xroden.flockr.features.expenses.presentation.BillFormState
import `in`.xroden.flockr.features.expenses.presentation.BillFormViewModel
import `in`.xroden.flockr.features.expenses.presentation.REMINDER_DAY_OPTIONS
import `in`.xroden.flockr.features.expenses.ui.SplitEditor
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.HeroColumn
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
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.inputs.OptionSheet
import `in`.xroden.flockr.ui.components.inputs.amountHint
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.relativeDayLabel
import `in`.xroden.flockr.utils.rememberHaptics

/** The pickers a sentence token can open; one at a time. */
private enum class Picker { FREQUENCY, DATE, CATEGORY, SPLIT, REMINDER, PREPAYMENT }

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
    var picker by remember { mutableStateOf<Picker?>(null) }

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
        bottomBar = {
            if (form.isLoaded) {
                FormSubmitBar(
                    text = if (form.isEditing) "Save changes" else "Add bill",
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
                FormHero(if (form.isEditing) "Edit bill" else "New bill") {
                    HeroAmountInput(
                        value = form.amount,
                        onValueChange = { amount -> viewModel.update { it.copy(amount = amount) } },
                        currencyCode = form.currencyCode,
                        enabled = isEnabled,
                        autoFocus = !form.isEditing,
                    )
                    val hint = amountHint(form.amount, form.currencyCode)
                    HeroNote(hint ?: shareNote(form), isError = hint != null)
                    HeroTextInput(
                        value = form.name,
                        onValueChange = { name -> viewModel.update { it.copy(name = name) } },
                        placeholder = "Rent, internet, electricity",
                        enabled = isEnabled,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                }
            },
        ) {
            ScheduleSentence(form, config, isEnabled, onPick = { picker = it })
            if (form.frequency == ExpenseFrequency.CUSTOM) {
                FlockrTextField(
                    value = form.customFrequencyDays,
                    onValueChange = { days -> viewModel.update { it.copy(customFrequencyDays = days) } },
                    label = "Every how many days",
                    keyboardType = KeyboardType.Number,
                    isError = form.customFrequencyDays.isNotEmpty() && form.parsedCustomDays == null,
                    supportingText = "From 1 to 366",
                    enabled = isEnabled,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                )
            }
            ReminderSentence(form, isEnabled, onPick = { picker = it })
            if (form.isEditing) {
                Text(
                    "The schedule counts from the first due date, so that date can't change once the bill exists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            SentenceNote(
                value = form.notes,
                onValueChange = { notes -> viewModel.update { it.copy(notes = notes) } },
                placeholder = "Account number, who to call when it's down",
                enabled = isEnabled,
            )
        }
    }

    when (picker) {
        Picker.FREQUENCY -> OptionSheet(
            options = ExpenseFrequency.entries,
            selected = form.frequency,
            onSelect = { frequency -> viewModel.update { it.copy(frequency = frequency) } },
            onDismiss = { picker = null },
            title = "How often is it due?",
            optionLabel = { it.label },
        )
        Picker.DATE -> form.firstDueDate?.let { date ->
            FlockrDatePickerDialog(
                initialDate = date,
                firstDayOfWeek = config?.firstDayOfWeek,
                onDateSelected = { due -> viewModel.update { it.copy(firstDueDate = due) }; picker = null },
                onDismiss = { picker = null },
            )
        }
        Picker.CATEGORY -> OptionSheet(
            options = ExpenseCategories.DEFAULT,
            selected = form.category,
            onSelect = { category -> viewModel.update { it.copy(category = category) } },
            onDismiss = { picker = null },
            title = "What kind of bill?",
            icon = ::categoryIcon,
        )
        Picker.REMINDER -> OptionSheet(
            options = listOf<Int?>(null) + REMINDER_DAY_OPTIONS,
            selected = form.reminderDaysBefore.takeIf { form.reminderEnabled },
            onSelect = { days -> viewModel.update { it.copy(reminderEnabled = days != null, reminderDaysBefore = days ?: it.reminderDaysBefore) } },
            onDismiss = { picker = null },
            title = "Remind the house at 9am",
            optionLabel = { days -> days?.let(::reminderLabel)?.replaceFirstChar(Char::uppercase) ?: "Don't remind anyone" },
        )
        Picker.PREPAYMENT -> OptionSheet(
            options = listOf(false, true),
            selected = form.allowPrepayment,
            onSelect = { allow -> viewModel.update { it.copy(allowPrepayment = allow) } },
            onDismiss = { picker = null },
            title = "When can it be paid?",
            optionLabel = { allow -> if (allow) "Any time, even early" else "From its reminder day on" },
        )
        Picker.SPLIT -> ModalBottomSheet(onDismissRequest = { picker = null }) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("How is each payment shared?", style = MaterialTheme.typography.titleLargeEmphasized)
                if (form.split.isEnabled) {
                    Text(
                        "Each payment is split this way, scaled to what was actually paid that time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
            }
        }
        null -> Unit
    }
}

/** "Repeats **monthly** from **today** for **Rent** split **equally between everyone**." */
@Composable
private fun ScheduleSentence(form: BillFormState, config: HouseConfig?, enabled: Boolean, onPick: (Picker) -> Unit) {
    Sentence {
        SentenceWords("Repeats")
        SentenceToken(frequencyLabel(form), onClick = { onPick(Picker.FREQUENCY) }, enabled = enabled, icon = Icons.Rounded.EventRepeat)
        form.firstDueDate?.let {
            SentenceWords("from")
            SentenceToken(it.relativeDayLabel(config), onClick = { onPick(Picker.DATE) }, enabled = enabled && !form.isEditing, icon = Icons.Rounded.CalendarMonth)
        }
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

/** "Remind the house **3 days before** and it can be paid **from the reminder day**." */
@Composable
private fun ReminderSentence(form: BillFormState, enabled: Boolean, onPick: (Picker) -> Unit) {
    Sentence {
        SentenceWords("Remind the house")
        if (form.reminderEnabled) {
            SentenceToken(reminderLabel(form.reminderDaysBefore), onClick = { onPick(Picker.REMINDER) }, enabled = enabled, icon = Icons.Rounded.Notifications)
        } else {
            SentenceToken("never", onClick = { onPick(Picker.REMINDER) }, enabled = enabled, icon = Icons.Rounded.NotificationsOff, isUnset = true)
        }
        SentenceWords("and it can be paid")
        SentenceToken(
            if (form.allowPrepayment) "any time" else "from the reminder day",
            onClick = { onPick(Picker.PREPAYMENT) },
            enabled = enabled,
            icon = Icons.Rounded.Payments,
        )
    }
}

private fun frequencyLabel(form: BillFormState): String =
    if (form.frequency == ExpenseFrequency.CUSTOM) {
        form.parsedCustomDays?.let { "every $it day${if (it == 1) "" else "s"}" } ?: "every few days"
    } else {
        form.frequency.label.lowercase()
    }

private fun reminderLabel(days: Int): String = if (days == 0) "on the day" else "$days day${if (days == 1) "" else "s"} before"

/** "equally between everyone", "by shares between Riya and you", and so on. */
private fun splitLabel(form: BillFormState): String {
    val active = form.members.filter { it.isActive }.map { it.userId }.toSet()
    val ids = form.split.participantIds
    val who = when {
        ids.isNotEmpty() && ids == active -> "everyone"
        ids.size == 1 -> "just ${nameFor(ids.single(), form)}"
        ids.size == 2 -> ids.joinToString(" and ") { nameFor(it, form) }
        else -> "${ids.size} people"
    }
    return "${form.split.method.phrase} between $who"
}

private fun nameFor(userId: String, form: BillFormState): String =
    if (userId == form.viewerId) "you" else form.members.firstOrNull { it.userId == userId }?.shortName ?: "someone"

/** What each payment means for the viewer under the current split, or a nudge while it's blank. */
private fun shareNote(form: BillFormState): String {
    val total = form.parsedAmount ?: return "Type the usual amount"
    if (!form.split.isEnabled) return "Whoever pays bears all of it"
    val mine = form.preview?.firstOrNull { it.userId == form.viewerId }?.owedShare
        ?: return "The split doesn't add up to ${total.formatMoney(form.currencyCode)} yet"
    return if (mine.signum() == 0) "You're not in this split" else "Your share is ${mine.formatMoney(form.currencyCode)} each time"
}
