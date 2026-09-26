/**
 * Logging a use of a per-diem item: the quantity typed large on cobalt with what it will cost, then
 * which item and when as a sentence.
 */
package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.PerDiemEntryFormState
import `in`.xroden.flockr.features.expenses.presentation.PerDiemEntryFormViewModel
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceNote
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.inputs.FlockrDatePickerDialog
import `in`.xroden.flockr.ui.components.inputs.OptionSheet
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.relativeDayLabel
import `in`.xroden.flockr.utils.rememberHaptics

private const val PLACEHOLDER_ALPHA = 0.45f

/** The pickers a sentence token can open; one at a time. */
private enum class EntryPicker { ITEM, DATE }

@Composable
fun PerDiemEntryFormScreen(
    houseId: String,
    configId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onAddItem: () -> Unit,
    viewModel: PerDiemEntryFormViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    val isEnabled = !form.isSaving
    var picker by remember { mutableStateOf<EntryPicker?>(null) }

    LaunchedEffect(houseId, configId) { viewModel.initialize(houseId, configId) }
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
            if (!form.isLoaded || form.items.isNotEmpty()) {
                FormSubmitBar(
                    text = form.cost?.let { "Log ${it.formatMoney(form.currencyCode)}" } ?: "Log",
                    onClick = viewModel::save,
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
        if (form.items.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.WaterDrop,
                title = "Nothing to log yet",
                subtitle = "Add what the house buys by use, like milk or water cans, with its price per unit.",
                actionText = "Add an item",
                onActionClick = onAddItem,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            hero = {
                FormHero("Log usage") {
                    QuantityInput(
                        value = form.quantity,
                        onValueChange = { quantity -> viewModel.update { it.copy(quantity = quantity) } },
                        unit = form.item?.unit.orEmpty(),
                        enabled = isEnabled,
                    )
                    val isInvalid = form.quantity.isNotEmpty() && form.parsedQuantity == null
                    HeroNote(if (isInvalid) "Use a number with up to three decimal places" else costNote(form), isError = isInvalid)
                }
            },
        ) {
            Sentence {
                SentenceWords("of")
                SentenceToken(
                    form.item?.itemName ?: "pick an item",
                    onClick = { picker = EntryPicker.ITEM },
                    enabled = isEnabled,
                    icon = form.item?.let { categoryIcon(it.category) },
                    isUnset = form.item == null,
                )
                form.date?.let {
                    SentenceWords("on")
                    SentenceToken(it.relativeDayLabel(config), onClick = { picker = EntryPicker.DATE }, enabled = isEnabled, icon = Icons.Rounded.CalendarMonth)
                }
            }
            SentenceNote(
                value = form.notes,
                onValueChange = { notes -> viewModel.update { it.copy(notes = notes) } },
                placeholder = "Guests over, extra for the party",
                enabled = isEnabled,
            )
        }
    }

    when (picker) {
        EntryPicker.ITEM -> OptionSheet(
            options = form.items,
            selected = form.item,
            onSelect = { chosen -> viewModel.update { it.copy(configId = chosen.id) } },
            onDismiss = { picker = null },
            title = "What did you use?",
            optionLabel = { "${it.itemName} · ${it.rate.formatMoney(form.currencyCode)} per ${it.unit}" },
            icon = { categoryIcon(it.category) },
        )
        EntryPicker.DATE -> form.date?.let { date ->
            FlockrDatePickerDialog(
                initialDate = date,
                firstDayOfWeek = config?.firstDayOfWeek,
                onDateSelected = { day -> viewModel.update { it.copy(date = day) }; picker = null },
                onDismiss = { picker = null },
            )
        }
        null -> Unit
    }
}

/** "That's ₹56 at ₹28 per litre", or a prompt while the quantity is blank. */
private fun costNote(form: PerDiemEntryFormState): String {
    val item = form.item ?: return "Pick what you used"
    val cost = form.cost ?: return "How much did you use?"
    return "That's ${cost.formatMoney(form.currencyCode)} at ${item.rate.formatMoney(form.currencyCode)} per ${item.unit}"
}

/**
 * The quantity typed onto the hero at display size with the item's unit after it, on a number pad.
 * The keyboard comes up on arrival, since the quantity is what this form is for.
 */
@Composable
private fun QuantityInput(value: String, onValueChange: (String) -> Unit, unit: String, enabled: Boolean) {
    val colors = MaterialTheme.flockrColors
    val style = MaterialTheme.typography.displayLargeEmphasized.copy(color = colors.onHero)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(colors.sun),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f, fill = false).focusRequester(focus).semantics { contentDescription = "Quantity" },
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("0", style = style, color = style.color.copy(alpha = PLACEHOLDER_ALPHA), maxLines = 1)
                    inner()
                }
            },
        )
        Text(unit, style = MaterialTheme.typography.displaySmallEmphasized, color = colors.onHeroVariant, maxLines = 1)
    }
}
