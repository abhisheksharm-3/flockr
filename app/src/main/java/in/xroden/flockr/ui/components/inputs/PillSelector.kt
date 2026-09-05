package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * Tab-like switching between mutually exclusive views, built on the Expressive [ButtonGroup].
 *
 * The group owns the connected-shape treatment: outer corners stay round, inner corners square
 * off, and pressing one button squeezes its neighbours aside. Wrapping the result in extra
 * padding or a background swallows that squeeze, so callers should pass layout modifiers only.
 *
 * Tabs that do not fit collapse into an overflow menu rather than shrinking past legibility.
 */
@Composable
fun PillSelector(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<Int>? = null
) {
    val haptics = rememberHaptics()

    ButtonGroup(
        overflowIndicator = { menuState -> OverflowIndicator(menuState) },
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = selectedIndex == index
            toggleableItem(
                checked = selected,
                label = counts?.getOrNull(index)?.let { "$title  $it" } ?: title,
                onCheckedChange = {
                    if (!selected) {
                        haptics.select()
                        onTabSelected(index)
                    }
                },
                weight = 1f
            )
        }
    }
}

@Composable
private fun OverflowIndicator(menuState: ButtonGroupMenuState) {
    val haptics = rememberHaptics()
    FilledIconButton(
        onClick = {
            haptics.tap()
            if (menuState.isShowing) menuState.dismiss() else menuState.show()
        }
    ) {
        Icon(Icons.Default.MoreVert, contentDescription = "More tabs")
    }
}

/**
 * A standalone on/off filter pill, for a toggle that is not one of a mutually exclusive set.
 */
@Composable
fun TogglePill(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptics = rememberHaptics()

    ToggleButton(
        checked = checked,
        onCheckedChange = {
            haptics.toggle(it)
            onCheckedChange(it)
        },
        modifier = modifier,
        enabled = enabled
    ) {
        Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
    }
}
