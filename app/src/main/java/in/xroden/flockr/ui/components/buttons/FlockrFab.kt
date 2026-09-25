/** The app's floating action buttons: one primary action, or several related actions behind a toggle. */
package `in`.xroden.flockr.ui.components.buttons

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.xroden.flockr.utils.rememberHaptics

private const val CollapseMenuDescription = "Close menu"

/**
 * The primary action of a list screen.
 *
 * [expanded] is animated by the framework, so a caller can drive it from scroll state and get the
 * label collapsing into the icon rather than a swap.
 */
@Composable
fun FlockrExtendedFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true
) {
    val haptics = rememberHaptics()

    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = { Icon(icon, contentDescription = if (expanded) null else text) },
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier,
        expanded = expanded
    )
}


/**
 * One entry in a [FlockrFabMenu].
 */
data class FabAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * A FAB that morphs into a column of labelled actions, for a screen whose primary affordance is
 * several related writes rather than one.
 *
 * The menu expands across the screen, so place it in a full-size `Box` aligned to the bottom end
 * rather than in the `Scaffold` FAB slot. Expansion state and back-to-dismiss are owned here, and
 * choosing an action collapses the menu before running it.
 */
@Composable
fun FlockrFabMenu(
    actions: List<FabAction>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add
) {
    val haptics = rememberHaptics()
    var expanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = expanded) { expanded = false }

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = {
                    haptics.toggle(it)
                    expanded = it
                }
            ) {
                val showClose = checkedProgress > 0.5f
                val animatedIcon = with(ToggleFloatingActionButtonDefaults) {
                    Modifier.animateIcon({ checkedProgress })
                }

                Icon(
                    imageVector = if (showClose) Icons.Default.Close else icon,
                    contentDescription = if (showClose) CollapseMenuDescription else contentDescription,
                    modifier = animatedIcon
                )
            }
        },
        modifier = modifier
    ) {
        actions.forEach { action ->
            FloatingActionButtonMenuItem(
                onClick = {
                    haptics.tap()
                    expanded = false
                    action.onClick()
                },
                text = { Text(action.text) },
                icon = { Icon(action.icon, contentDescription = null) }
            )
        }
    }
}
