/** Picking one value from a fixed list, shown as a read-only field that opens a menu. */
package `in`.xroden.flockr.ui.components.inputs

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
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * A field showing [selected] that opens a menu of [options] on tap.
 *
 * [optionLabel] turns an option into the text shown, so the same field serves categories, repeat
 * frequencies, currencies and date layouts. Choosing is a selection, so it fires [select] haptics.
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
