/**
 * Adding or editing a per-diem item: its name typed large on cobalt, its price and category as a
 * sentence, and quiet rows for retiring it.
 */
package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Straighten
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.PerDiemCategories
import `in`.xroden.flockr.features.expenses.presentation.PerDiemItemFormViewModel
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.inputs.OptionSheet
import `in`.xroden.flockr.ui.components.inputs.amountHint
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.rememberHaptics

/** The pickers a sentence token can open; one at a time. */
private enum class ItemPicker { PRICE, UNIT, CATEGORY }

@Composable
fun PerDiemItemFormScreen(
    houseId: String,
    configId: String?,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: PerDiemItemFormViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }
    var picker by remember { mutableStateOf<ItemPicker?>(null) }
    val isEnabled = !form.isSaving
    val unit = form.unit.trim().ifEmpty { "unit" }

    LaunchedEffect(houseId, configId) { viewModel.initialize(houseId, configId) }
    LaunchedEffect(Unit) {
        viewModel.done.collect {
            haptics.success()
            onDone()
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
                    text = if (form.isEditing) "Save changes" else "Add item",
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
                FormHero(if (form.isEditing) "Edit item" else "New item") {
                    HeroTextInput(
                        value = form.itemName,
                        onValueChange = { name -> viewModel.update { it.copy(itemName = name) } },
                        placeholder = "Milk, water can, newspaper",
                        enabled = isEnabled,
                        autoFocus = !form.isEditing,
                    )
                    val hint = amountHint(form.rate, form.currencyCode)
                    HeroNote(
                        text = hint ?: if (form.isEditing) {
                            "A new price counts from now on. What's already logged keeps its price."
                        } else {
                            "Something the house buys by use and pays for at month end"
                        },
                        isError = hint != null,
                    )
                }
            },
        ) {
            Sentence {
                SentenceWords("Costs")
                val rate = form.parsedRate
                SentenceToken(
                    rate?.formatMoney(form.currencyCode) ?: "set a price",
                    onClick = { picker = ItemPicker.PRICE },
                    enabled = isEnabled,
                    icon = Icons.Rounded.Payments,
                    isUnset = rate == null,
                )
                SentenceWords("per")
                SentenceToken(unit, onClick = { picker = ItemPicker.UNIT }, enabled = isEnabled, icon = Icons.Rounded.Straighten, isUnset = form.unit.isBlank())
                SentenceWords("filed under")
                SentenceToken(form.category, onClick = { picker = ItemPicker.CATEGORY }, enabled = isEnabled, icon = categoryIcon(form.category))
            }
            if (form.isEditing) {
                Column {
                    ListRow(
                        headline = "Archive ${form.itemName.trim().ifEmpty { "item" }}",
                        supporting = "Hides it from logging and keeps every past use",
                        leading = { IconBadge(Icons.Rounded.Archive, BadgeTone.SLATE) },
                        onClick = { haptics.tap(); viewModel.archive() }.takeIf { isEnabled },
                    )
                    if (form.isAdmin) {
                        ListRow(
                            headline = "Delete it and all its usage",
                            supporting = "Gone from every month. Billed months keep their bills.",
                            leading = { IconBadge(Icons.Rounded.DeleteForever, BadgeTone.ROSE) },
                            onClick = { isConfirmingDelete = true }.takeIf { isEnabled },
                            headlineColor = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    when (picker) {
        ItemPicker.PRICE -> FieldSheet(title = "What does one $unit cost?", onDone = { picker = null }) {
            AmountField(
                value = form.rate,
                onValueChange = { rate -> viewModel.update { it.copy(rate = rate) } },
                currencyCode = form.currencyCode,
                label = "Price per $unit",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ItemPicker.UNIT -> FieldSheet(title = "How is it counted?", onDone = { picker = null }) {
            FlockrTextField(
                value = form.unit,
                onValueChange = { text -> viewModel.update { it.copy(unit = text) } },
                label = "Unit",
                placeholder = "litre, can, packet",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        ItemPicker.CATEGORY -> OptionSheet(
            options = PerDiemCategories.DEFAULT,
            selected = form.category,
            onSelect = { category -> viewModel.update { it.copy(category = category) } },
            onDismiss = { picker = null },
            title = "Where does it belong?",
            icon = ::categoryIcon,
        )
        null -> Unit
    }

    if (isConfirmingDelete) {
        ConfirmDialog(
            title = "Delete ${form.itemName} and its usage?",
            message = "Every logged use of it goes too, in every month. Months already billed keep their bills.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                isConfirmingDelete = false
                viewModel.deleteWithUsage()
            },
            onDismiss = { isConfirmingDelete = false },
        )
    }
}

/** A sheet holding one typed value, above the keyboard, closed by its Done button or a swipe. */
@Composable
private fun FieldSheet(title: String, onDone: () -> Unit, field: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDone) {
        Column(
            Modifier.navigationBarsPadding().imePadding().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
            field()
            FlockrPrimaryButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}
