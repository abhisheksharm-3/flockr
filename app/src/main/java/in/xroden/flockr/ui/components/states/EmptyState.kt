package `in`.xroden.flockr.ui.components.states

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.ui.theme.*

/**
 * Standardized empty state component for consistent UX across all screens.
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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.xxxl)
        ) {
            Surface(
                modifier = Modifier.size(IconSize.display),
                shape = RoundedCornerShape(CornerRadius.xxl),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.xl),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            )

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(CornerRadius.md),
                    contentPadding = PaddingValues(horizontal = Spacing.xxl, vertical = Spacing.md)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Standardized error state component for consistent error UX.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.xxxl)
        ) {
            Surface(
                modifier = Modifier.size(IconSize.display),
                shape = RoundedCornerShape(CornerRadius.xxl),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.xl),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Retry", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
