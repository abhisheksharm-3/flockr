package `in`.xroden.flockr.ui.components.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard Flockr list item using Material 3 ListItem
 */
@Composable
fun FlockrListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = headlineContent,
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        },
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Convenience wrapper for text-based list items
 */
@Composable
fun FlockrListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    overlineText: String? = null,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    FlockrListItem(
        headlineContent = {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.titleMedium
            )
        },
        modifier = modifier,
        overlineContent = overlineText?.let { {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall
            )
        } },
        supportingContent = supportingText?.let { {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } },
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        onClick = onClick
    )
}

/**
 * Divider between list items
 */
@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    )
}

