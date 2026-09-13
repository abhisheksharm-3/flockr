package `in`.xroden.flockr.ui.components.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * The screen-filling "there is nothing here yet" state.
 *
 * [actionText] and [onActionClick] are read as a pair: the button appears only when both are
 * given, and it owns its own tap haptic, so a caller must not fire a second one.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    StateLayout(modifier) {
        StateSilhouette(icon = icon, containerColor = MaterialTheme.colorScheme.primaryContainer)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (actionText != null && onActionClick != null) {
            Button(onClick = { haptics.tap(); onActionClick() }) {
                Text(actionText)
            }
        }
    }
}

/**
 * The screen-filling failure state. The silhouette carries the error colour so [message] can stay
 * on the surface content colour and remain readable at length.
 *
 * The retry button owns its own tap haptic; a caller must not fire a second one.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val haptics = rememberHaptics()
    StateLayout(modifier) {
        StateSilhouette(
            icon = Icons.Rounded.ErrorOutline,
            containerColor = MaterialTheme.colorScheme.errorContainer
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        onRetry?.let { retry ->
            OutlinedButton(onClick = { haptics.tap(); retry() }) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun StateLayout(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(Spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            content = content
        )
    }
}

/**
 * The icon on an Expressive [MaterialShapes] silhouette. One shape across every state, so the
 * colour is what tells an empty screen apart from a broken one.
 */
@Composable
private fun StateSilhouette(icon: ImageVector, containerColor: Color) {
    Surface(
        modifier = Modifier.size(IconSize.display),
        shape = MaterialShapes.Cookie9Sided.toShape(),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(IconSize.xl)
            )
        }
    }
}
