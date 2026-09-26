/** How a balance reads and looks, the same on every screen. */
package `in`.xroden.flockr.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import `in`.xroden.flockr.ui.theme.flockrColors
import java.math.BigDecimal

/** The colour for a balance: positive when others owe the viewer, negative when the viewer owes. */
@Composable
fun balanceColor(net: BigDecimal): Color = when (net.signum()) {
    1 -> MaterialTheme.flockrColors.positive
    -1 -> MaterialTheme.flockrColors.negative
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * The words that go above a balance's amount, so the direction never rests on colour alone. A zero
 * balance has no amount to show, so its phrase stands on its own.
 */
fun balanceHeadline(net: BigDecimal): String = when (net.signum()) {
    1 -> "You're owed"
    -1 -> "You owe"
    else -> "You're all settled up"
}
