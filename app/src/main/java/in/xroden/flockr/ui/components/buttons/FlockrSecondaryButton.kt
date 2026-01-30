package `in`.xroden.flockr.ui.components.buttons

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.ui.theme.*

/**
 * Secondary outlined button with consistent styling.
 */
@Composable
fun FlockrSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.xxl, vertical = Spacing.md)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = contentPadding
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSize.sm),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.sm)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Tertiary/text button with consistent styling.
 */
@Composable
fun FlockrTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Danger button for destructive actions.
 */
@Composable
fun FlockrDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    outlined: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled && !isLoading,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            contentPadding = PaddingValues(horizontal = Spacing.xxl, vertical = Spacing.md)
        ) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.error)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.scale(scale),
            enabled = enabled && !isLoading,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            contentPadding = PaddingValues(horizontal = Spacing.xxl, vertical = Spacing.md)
        ) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.onError)
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    color: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(IconSize.sm),
            strokeWidth = 2.dp,
            color = color
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.sm)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
