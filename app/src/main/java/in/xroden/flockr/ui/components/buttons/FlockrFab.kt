package `in`.xroden.flockr.ui.components.buttons

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.xroden.flockr.ui.theme.*
import `in`.xroden.flockr.utils.rememberHapticFeedback

/**
 * Standardized Extended FAB for list screens.
 */
@Composable
fun FlockrExtendedFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true
) {
    val haptics = rememberHapticFeedback()

    if (expanded) {
        ExtendedFloatingActionButton(
            onClick = {
                haptics.performClick()
                onClick()
            },
            icon = { Icon(icon, contentDescription = null) },
            text = { Text(text) },
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = Elevation.lg,
                pressedElevation = Elevation.xl
            )
        )
    } else {
        FloatingActionButton(
            onClick = {
                haptics.performClick()
                onClick()
            },
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = Elevation.lg,
                pressedElevation = Elevation.xl
            )
        ) {
            Icon(icon, contentDescription = text)
        }
    }
}

/**
 * Standardized small FAB for secondary actions.
 */
@Composable
fun FlockrSmallFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val haptics = rememberHapticFeedback()

    SmallFloatingActionButton(
        onClick = {
            haptics.performClick()
            onClick()
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = Elevation.sm,
            pressedElevation = Elevation.md
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
