/** The app's one list row, and the tinted icon tile that leads many of them. */
package `in`.xroden.flockr.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors

/**
 * A full-width row on the page: [leading] (an avatar or [IconBadge]), a headline with an optional
 * supporting line, and [trailing] (usually an amount). Rows sit straight on the page and the list's
 * rhythm separates them, so there is no divider or card. [onClick] makes the whole row the target.
 */
@Composable
fun ListRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    headlineColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = ComponentHeight.listItemCompact)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(headline, style = MaterialTheme.typography.titleSmall, color = headlineColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}

/**
 * An amount with a short line under it, right-aligned at a row's end, such as "₹1,240" over
 * "you owe ₹413.33". [detailColor] carries the direction; the words carry it too.
 */
@Composable
fun TrailingAmount(amount: String, detail: String? = null, detailColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(amount, style = MaterialTheme.typography.titleSmallEmphasized, maxLines = 1)
        detail?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = detailColor, maxLines = 1) }
    }
}

/** The tints an [IconBadge] can take, so a screen can tell kinds of thing apart without inventing colours. */
enum class BadgeTone { COBALT, JADE, SUN, SLATE, ROSE }

/** A tinted circle holding an icon, tinted by [tone]. Decorative: the row's text names the thing. */
@Composable
fun IconBadge(icon: ImageVector, tone: BadgeTone = BadgeTone.COBALT, size: Dp = ComponentHeight.avatar) {
    val (container, content) = badgeColors(tone)
    Surface(modifier = Modifier.size(size), shape = CircleShape, color = container, contentColor = content) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.sm + Spacing.xxs)) }
    }
}

@Composable
private fun badgeColors(tone: BadgeTone): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (tone) {
        BadgeTone.COBALT -> primaryContainer to onPrimaryContainer
        BadgeTone.JADE -> tertiaryContainer to onTertiaryContainer
        BadgeTone.SUN -> MaterialTheme.flockrColors.sun to MaterialTheme.flockrColors.onSun
        BadgeTone.SLATE -> secondaryContainer to onSecondaryContainer
        BadgeTone.ROSE -> errorContainer to onErrorContainer
    }
}
