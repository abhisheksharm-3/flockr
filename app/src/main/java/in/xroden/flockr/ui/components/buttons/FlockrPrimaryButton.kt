/** The app's primary call to action. */
package `in`.xroden.flockr.ui.components.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.xroden.flockr.utils.rememberHaptics

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

