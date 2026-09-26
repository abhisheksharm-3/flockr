/** Choosing one option from a bottom sheet, searchable when the list is long. */
package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics
import kotlinx.coroutines.launch

/** Past this many options a list is long enough to need a search field. */
internal const val SEARCH_THRESHOLD = 12

/**
 * A sheet of [options] with [selected] ticked; choosing one calls [onSelect] and closes the sheet.
 *
 * Labels are worked out once, when the sheet opens, so typing in the search filters a ready list
 * instead of rebuilding every label per keystroke, and the list lays out only the rows on screen,
 * so it opens at once however long it is. [icon] puts a leading glyph on each row.
 */
@Composable
fun <T> OptionSheet(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    optionLabel: (T) -> String = { it.toString() },
    icon: ((T) -> ImageVector)? = null,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = options.size > SEARCH_THRESHOLD)
    var query by remember { mutableStateOf("") }
    val labelled = remember(options) { options.map { it to optionLabel(it) } }
    val shown = remember(labelled, query) {
        val needle = query.trim()
        if (needle.isEmpty()) labelled else labelled.filter { (_, text) -> text.contains(needle, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        title?.let {
            Text(it, style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))
        }
        if (options.size > SEARCH_THRESHOLD) {
            FlockrTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search",
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                capitalization = KeyboardCapitalization.None,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
        LazyColumn(Modifier.fillMaxWidth().imePadding().navigationBarsPadding()) {
            items(shown) { (option, text) ->
                ListRow(
                    headline = text,
                    leading = icon?.let { glyph -> { IconBadge(glyph(option), BadgeTone.COBALT) } },
                    trailing = if (option == selected) ({ Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }) else null,
                    onClick = {
                        haptics.select()
                        onSelect(option)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                )
            }
        }
    }
}
