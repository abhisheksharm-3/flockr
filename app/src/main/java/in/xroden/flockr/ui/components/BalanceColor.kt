/** The colour a balance is shown in, the same on every screen. */
package `in`.xroden.flockr.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.math.BigDecimal

/** The colour for a balance: the primary colour when others owe, the error colour when you owe. */
@Composable
fun balanceColor(net: BigDecimal): Color = when (net.signum()) {
    1 -> MaterialTheme.colorScheme.primary
    -1 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
