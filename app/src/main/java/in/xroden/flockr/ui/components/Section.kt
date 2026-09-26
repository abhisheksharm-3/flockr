/** The page's grouping vocabulary: titled sections that sit straight on the page, never in a card. */
package `in`.xroden.flockr.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import `in`.xroden.flockr.ui.theme.Spacing

/**
 * A group of related content under a small heading, straight on the page with no container.
 *
 * The heading is inset by the page gutter; [content] is not, so full-width rows can run edge to edge
 * and their own padding lines them up with the heading. [action] sits at the heading's end, usually
 * a text button such as "See all".
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle(title, subtitle, action)
        content()
    }
}

/** A section heading on its own, for lists that lay their rows out as separate lazy items. */
@Composable
fun SectionTitle(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = SectionTitleHeight).padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(bottom = Spacing.xs)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMediumEmphasized.copy(letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 2),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        action?.invoke()
    }
}

private val SectionTitleHeight = Spacing.xxxxl
