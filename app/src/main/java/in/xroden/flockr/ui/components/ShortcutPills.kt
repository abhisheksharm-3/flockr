/** A swipeable row of pill buttons that jump to the parts of a screen's world, such as a house's shopping or a ledger's bills. */
package `in`.xroden.flockr.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors

/** One pill. [count] badges it in sun when something there is waiting; [isPrimary] fills it in cobalt as the row's main action. */
data class Shortcut(val title: String, val icon: ImageVector, val onClick: () -> Unit, val count: Int? = null, val isPrimary: Boolean = false)

/**
 * [shortcuts] as Expressive buttons, so each morphs its shape under the finger, in a row that
 * scrolls sideways when it outgrows the screen. They only navigate, so they fire no haptic.
 */
@Composable
fun ShortcutPills(shortcuts: List<Shortcut>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        shortcuts.forEach { shortcut ->
            val content: @Composable () -> Unit = {
                Icon(shortcut.icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(shortcut.title, style = MaterialTheme.typography.labelLargeEmphasized, maxLines = 1)
                shortcut.count?.takeIf { it > 0 }?.let {
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Badge(containerColor = MaterialTheme.flockrColors.sun, contentColor = MaterialTheme.flockrColors.onSun) { Text("$it") }
                }
            }
            val pill = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight)
            if (shortcut.isPrimary) {
                Button(onClick = shortcut.onClick, shapes = ButtonDefaults.shapes(), modifier = pill) { content() }
            } else {
                FilledTonalButton(onClick = shortcut.onClick, shapes = ButtonDefaults.shapes(), modifier = pill) { content() }
            }
        }
    }
}
