/** The first screen a signed-out visitor sees: what Flockr is for, and the way in. */
package `in`.xroden.flockr.features.auth.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.xroden.flockr.ui.components.HeroBackdrop
import `in`.xroden.flockr.ui.components.LightStatusBarIcons
import `in`.xroden.flockr.ui.components.loading.FlockrLockup
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors

private val ButtonHeight = ButtonDefaults.MediumContainerHeight
private const val GLYPH_FILL_ALPHA = 0.16f

/**
 * The whole screen is the hero cobalt with the flock, with the one sun-yellow button as the way in.
 * Both buttons only navigate, so neither fires a haptic.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
) {
    val colors = MaterialTheme.flockrColors
    LightStatusBarIcons()
    Box(Modifier.fillMaxSize()) {
        HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Spacing.xxl, vertical = Spacing.xl)) {
            FlockrLockup(markSize = IconSize.lg, style = MaterialTheme.typography.titleLargeEmphasized)
            Spacer(Modifier.weight(1f))
            Text("Your home, sorted.", style = MaterialTheme.typography.displayMediumEmphasized, color = colors.onHero)
            Text(
                "Split the bills, share the chores, stay friends.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onHeroVariant,
                modifier = Modifier.padding(top = Spacing.md),
            )
            Column(Modifier.padding(top = Spacing.xxxl), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ValueLine(Icons.AutoMirrored.Rounded.ReceiptLong, "Who owes whom, right to the paisa")
                ValueLine(Icons.Rounded.CleaningServices, "Chores that take turns on their own")
                ValueLine(Icons.Rounded.Groups, "Lists, documents and chat for the house")
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onGetStarted,
                shapes = ButtonDefaults.shapesFor(ButtonHeight),
                colors = ButtonDefaults.buttonColors(containerColor = colors.sun, contentColor = colors.onSun),
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonHeight),
            ) {
                Text("Get started", style = MaterialTheme.typography.titleMediumEmphasized)
            }
            TextButton(
                onClick = onSignIn,
                shapes = ButtonDefaults.shapesFor(ButtonHeight),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onHero),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm).heightIn(min = ButtonHeight),
            ) {
                Text("I already have an account", style = MaterialTheme.typography.titleMediumEmphasized)
            }
        }
    }
}

/** One thing Flockr does, after its glyph in a faint full circle on the cobalt. */
@Composable
private fun ValueLine(icon: ImageVector, text: String) {
    val colors = MaterialTheme.flockrColors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Surface(shape = CircleShape, color = colors.onHero.copy(alpha = GLYPH_FILL_ALPHA), contentColor = colors.onHero, modifier = Modifier.size(ComponentHeight.avatar)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.sm + Spacing.xxs)) }
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = colors.onHero)
    }
}
