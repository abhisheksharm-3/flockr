/** The app-lock screen, drenched in the hero cobalt with the flock, the same family as the launch loader. */
package `in`.xroden.flockr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import `in`.xroden.flockr.ui.components.loading.FlockrLockup
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.rememberHaptics

private val UnlockButtonHeight = ButtonDefaults.MediumContainerHeight

/**
 * The app-lock screen, drawn over the whole app while it is locked.
 *
 * The backing [Surface] swallows touches, so nothing underneath the overlay stays reachable.
 * [onUnlockClick] is expected to raise the system biometric prompt, which owns the screen from
 * that point on, so there is no in-flight state to show here; the lock only wiggles to answer the tap.
 */
@Composable
fun LockScreenOverlay(onUnlockClick: () -> Unit) {
    val haptics = rememberHaptics()
    val colors = MaterialTheme.flockrColors
    var unlockTaps by remember { mutableIntStateOf(0) }
    LightStatusBarIcons()
    Surface(modifier = Modifier.fillMaxSize(), color = colors.hero, contentColor = colors.onHero) {
        Box {
            HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = Spacing.xxl, vertical = Spacing.xl)) {
                FlockrLockup(markSize = IconSize.lg, style = MaterialTheme.typography.titleLargeEmphasized)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
                ) {
                    Surface(shape = CircleShape, color = colors.sun, contentColor = colors.onSun, modifier = Modifier.size(IconSize.display)) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedGlyph(Icons.Rounded.Lock, trigger = unlockTaps, motion = GlyphMotion.WIGGLE, contentDescription = null, modifier = Modifier.size(IconSize.xl))
                        }
                    }
                    Text("Flockr is locked", style = MaterialTheme.typography.displaySmallEmphasized, modifier = Modifier.padding(top = Spacing.lg))
                    Text("Unlock to get back to your houses.", style = MaterialTheme.typography.bodyLarge, color = colors.onHeroVariant)
                }
                Button(
                    onClick = {
                        haptics.tap()
                        unlockTaps++
                        onUnlockClick()
                    },
                    shapes = ButtonDefaults.shapesFor(UnlockButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.sun, contentColor = colors.onSun),
                    modifier = Modifier.fillMaxWidth().heightIn(min = UnlockButtonHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(UnlockButtonHeight),
                ) {
                    Text("Unlock", style = MaterialTheme.typography.titleMediumEmphasized)
                }
            }
        }
    }
}
