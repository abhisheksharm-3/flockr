/** The first screen a signed-out visitor sees: what Flockr is for, and the way in. */
package `in`.xroden.flockr.features.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import `in`.xroden.flockr.R
import `in`.xroden.flockr.ui.theme.Spacing

private val LogoSize = 72.dp
private val ButtonHeight = ButtonDefaults.MediumContainerHeight
private const val DARK_LUMINANCE = 0.5f
private const val ICON_CONTAINER_ALPHA = 0.12f

/**
 * Full-bleed over the house photo, faded into the theme's background so the copy stays readable in
 * both themes. [backgroundImageUrl] replaces the bundled photo when given. Both buttons only
 * navigate, so neither fires a haptic.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    backgroundImageUrl: String? = null
) {
    val background = MaterialTheme.colorScheme.background
    val isDark = background.luminance() < DARK_LUMINANCE

    Box(Modifier.fillMaxSize().background(background)) {
        if (backgroundImageUrl != null) {
            AsyncImage(model = backgroundImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Image(
                painter = painterResource(if (isDark) R.drawable.welcome_bg_dark else R.drawable.welcome_bg_light),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 0.3f),
                    0.5f to background.copy(alpha = 0.85f),
                    1f to background,
                )
            )
        )
        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Spacer(Modifier.weight(1f))
            Image(painter = painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(LogoSize))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Flockr", style = MaterialTheme.typography.displayMediumEmphasized, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Split bills, share chores and keep everyone at home on the same page.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier.padding(vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                FeatureRow(Icons.AutoMirrored.Rounded.ReceiptLong, "Shared costs", "See who owes whom and settle up in a tap")
                FeatureRow(Icons.Rounded.CleaningServices, "Chores that rotate", "Everyone takes their turn, and the app remembers")
                FeatureRow(Icons.Rounded.Groups, "One place for the house", "Lists, documents and chat for everyone who lives there")
            }
            Button(
                onClick = onGetStarted,
                shapes = ButtonDefaults.shapesFor(ButtonHeight),
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonHeight),
            ) {
                Text("Get started", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            OutlinedButton(
                onClick = onSignIn,
                shapes = ButtonDefaults.shapesFor(ButtonHeight),
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonHeight),
            ) {
                Text("I already have an account", style = MaterialTheme.typography.titleMediumEmphasized)
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Surface(
            shape = MaterialShapes.Cookie4Sided.toShape(),
            color = MaterialTheme.colorScheme.primary.copy(alpha = ICON_CONTAINER_ALPHA),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(Spacing.sm))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onBackground)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
