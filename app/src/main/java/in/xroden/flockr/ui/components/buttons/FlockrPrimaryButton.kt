/**
 * The app's primary call to action, plain and in the split form that hangs a menu of variants off
 * the same button.
 */
package `in`.xroden.flockr.ui.components.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.xroden.flockr.ui.theme.Motion
import `in`.xroden.flockr.utils.rememberHaptics

private const val VariantsMenuDescription = "More options"

private val PrimaryButtonHeight = ButtonDefaults.MediumContainerHeight

/**
 * The screen's main commitment: sign in, save, pay.
 *
 * Everything about the size — shape, content padding, icon size and spacing — is read off the
 * Expressive medium button scale, so the button stays in proportion if that scale moves. The press
 * feedback is the framework's shape morph, driven by the theme's motion scheme.
 *
 * [isLoading] disables the button and puts a loading indicator where the icon would go. The label
 * stays, so callers that swap [text] for a progress phrase still read correctly to a screen reader.
 */
@Composable
fun FlockrPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val haptics = rememberHaptics()

    Button(
        onClick = {
            haptics.tap()
            onClick()
        },
        shapes = ButtonDefaults.shapesFor(PrimaryButtonHeight),
        modifier = modifier.heightIn(min = PrimaryButtonHeight),
        enabled = enabled && !isLoading,
        contentPadding = ButtonDefaults.contentPaddingFor(PrimaryButtonHeight)
    ) {
        val iconSize = ButtonDefaults.iconSizeFor(PrimaryButtonHeight)
        val iconSpacing = ButtonDefaults.iconSpacingFor(PrimaryButtonHeight)

        if (isLoading) {
            LoadingIndicator(modifier = Modifier.size(iconSize), color = LocalContentColor.current)
            Spacer(Modifier.width(iconSpacing))
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(iconSpacing))
        }

        Text(text = text, style = MaterialTheme.typography.titleMediumEmphasized)
    }
}

/**
 * A primary action with its variants attached: the label runs [onClick], the chevron opens
 * [menuItems] beneath the button.
 *
 * [menuItems] is composed inside a [DropdownMenu] and is handed the callback that closes it, so an
 * item can dismiss the menu as part of acting. Items own their own haptics — this button fires only
 * for the leading action and for the chevron.
 */
@Composable
fun FlockrSplitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuItems: @Composable (dismiss: () -> Unit) -> Unit
) {
    val haptics = rememberHaptics()
    var menuExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        animationSpec = Motion.spatialFast
    )

    Box(modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = {
                        haptics.tap()
                        onClick()
                    },
                    enabled = enabled
                ) {
                    Text(text)
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = {
                        haptics.toggle(it)
                        menuExpanded = it
                    },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = VariantsMenuDescription,
                        modifier = Modifier.rotate(chevronRotation)
                    )
                }
            }
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            menuItems { menuExpanded = false }
        }
    }
}
