/** Picking one value from a fixed list, shown as a read-only field that opens a menu, or a searchable sheet for long lists. */
package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * A field showing [selected] that opens its [options] on tap: a dropdown for a short list, and a
 * bottom sheet with search for a long one such as time zones. The sheet lays out only the rows on
 * screen, so it opens at once however long the list is.
 *
 * [optionLabel] turns an option into the text shown, so the same field serves categories, repeat
 * frequencies, currencies and date layouts. Choosing is a selection, so it fires select haptics.
 */
@Composable
fun <T> ChoiceField(
    label: String,
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { it.toString() },
) {
    if (options.size > SEARCH_THRESHOLD) {
        SheetChoiceField(label, selected, options, onSelect, modifier, enabled, optionLabel)
    } else {
        MenuChoiceField(label, selected, options, onSelect, modifier, enabled, optionLabel)
    }
}

@Composable
private fun <T> MenuChoiceField(
    label: String,
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    optionLabel: (T) -> String,
) {
    val haptics = rememberHaptics()
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it && enabled },
        modifier = modifier,
    ) {
        FlockrTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        haptics.select()
                        onSelect(option)
                        isExpanded = false
                    },
                )
            }
        }
    }
}

/** The long-list form. A read-only text field swallows taps, so a clear layer on top of it is what opens the sheet. */
@Composable
private fun <T> SheetChoiceField(
    label: String,
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    optionLabel: (T) -> String,
) {
    var isOpen by remember { mutableStateOf(false) }

    Box(modifier) {
        FlockrTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOpen) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable(enabled = enabled, role = Role.DropdownList, onClickLabel = "Choose $label") { isOpen = true })
    }

    if (isOpen) {
        OptionSheet(options, selected, onSelect, onDismiss = { isOpen = false }, title = label, optionLabel = optionLabel)
    }
}
