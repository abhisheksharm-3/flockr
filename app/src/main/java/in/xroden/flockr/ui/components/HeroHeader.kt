/** The cobalt block a screen opens with when it has one number or sentence that matters most. */
package `in`.xroden.flockr.ui.components

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import coil3.compose.AsyncImage
import org.maplibre.android.geometry.LatLng
import `in`.xroden.flockr.utils.formatMoney
import java.math.BigDecimal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * A full-bleed cobalt header that runs up under the status bar, with a title, optional actions, and
 * whatever the screen leads with in [content]: a balance, a greeting, a single sentence.
 *
 * Put it first in the screen's scrolling content, in a `Scaffold` with no top bar, so it scrolls
 * away as the list moves. While it is on screen the status bar icons turn light, since dark icons
 * on cobalt are unreadable in both themes.
 *
 * With [imageUrl] the photo fills the whole block under a cobalt wash that deepens towards the
 * bottom, where the figures sit, so text keeps its contrast on any photo. Without one, a few soft
 * overlapping circles, the flock, give the cobalt some depth.
 */
@Composable
fun HeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    imageUrl: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    LightStatusBarIcons()
    val colors = MaterialTheme.flockrColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.hero,
        contentColor = colors.onHero,
        shape = RoundedCornerShape(bottomStart = HeroCorner, bottomEnd = HeroCorner),
    ) {
        Box {
            HeroBackdrop(imageUrl, Modifier.matchParentSize())
            Column(Modifier.statusBarsPadding().padding(bottom = Spacing.xxl)) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = ComponentHeight.listItemCompact).padding(horizontal = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(start = Spacing.md)) {
                        Text(title, style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        subtitle?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onHeroVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    actions()
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    content = content,
                )
            }
        }
    }
}

/**
 * The hero's ground, in order of preference: [imageUrl]'s photo; else, given [location], the real
 * streets around the house; else plain cobalt with the flock. A photo or map sits
 * under a cobalt wash that deepens towards the bottom, where figures sit, so white text keeps its
 * contrast; the map's wash starts lighter so its streets still read. Size it with the
 * modifier; it draws nothing else.
 */
@Composable
fun HeroBackdrop(imageUrl: String?, modifier: Modifier = Modifier, location: LatLng? = null) {
    val colors = MaterialTheme.flockrColors
    Box(modifier.background(colors.hero)) {
        when {
            !imageUrl.isNullOrBlank() -> {
                AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Wash(PhotoWashTop, PhotoWashBottom)
            }
            location != null -> {
                HouseMap(location, Modifier.matchParentSize())
                Wash(MapWashTop, PhotoWashBottom)
            }
            else -> Flock(Modifier.matchParentSize(), colors.onHero)
        }
    }
}

@Composable
private fun BoxScope.Wash(top: Float, bottom: Float) {
    val hero = MaterialTheme.flockrColors.hero
    Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(hero.copy(alpha = top), hero.copy(alpha = bottom)))))
}

/** Three soft circles off the top-right corner, drawn in [ink] at a whisper of opacity. */
@Composable
private fun Flock(modifier: Modifier, ink: Color) {
    Canvas(modifier) {
        val unit = size.width
        drawCircle(ink.copy(alpha = FlockAlpha), radius = unit * 0.42f, center = Offset(unit * 0.98f, unit * 0.02f))
        drawCircle(ink.copy(alpha = FlockAlpha), radius = unit * 0.28f, center = Offset(unit * 0.72f, unit * 0.28f))
        drawCircle(ink.copy(alpha = FlockAlpha * 1.4f), radius = unit * 0.16f, center = Offset(unit * 0.96f, unit * 0.48f))
    }
}

/** The label above a hero's number, in the hero's quieter text colour. */
@Composable
fun HeroLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.flockrColors.onHeroVariant, modifier = Modifier.padding(top = Spacing.sm))
}

/** The hero's headline figure. */
@Composable
fun HeroAmount(text: String) {
    Text(text, style = MaterialTheme.typography.displayMediumEmphasized, maxLines = 1)
}

/**
 * An amount that counts up from zero when it first appears, easing out fast so it lands quickly.
 * It ends on [amount] exactly, and a screen reader hears only that final figure. With the system's
 * animations turned off the count is skipped.
 */
@Composable
fun HeroCountUp(amount: BigDecimal, currencyCode: String) {
    val progress = remember(amount) { Animatable(0f) }
    LaunchedEffect(amount) { progress.animateTo(1f, tween(CountUpMillis, easing = EaseOutExpo)) }
    val final = amount.formatMoney(currencyCode)
    val shown = if (progress.value >= 1f) final else (amount * progress.value.toBigDecimal()).formatMoney(currencyCode)
    Text(
        shown,
        style = MaterialTheme.typography.displayMediumEmphasized,
        maxLines = 1,
        modifier = Modifier.clearAndSetSemantics { contentDescription = final },
    )
}

/** A small sun-yellow pill on the hero, for good news such as "All square". */
@Composable
fun HeroBadge(text: String) {
    val colors = MaterialTheme.flockrColors
    Surface(color = colors.sun, contentColor = colors.onSun, shape = CircleShape, modifier = Modifier.padding(top = Spacing.sm)) {
        Text(text, style = MaterialTheme.typography.labelLargeEmphasized, modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs))
    }
}

/** A supporting sentence under the hero's figure. */
@Composable
fun HeroCaption(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.flockrColors.onHeroVariant)
}

/** A row of hero buttons, spaced and set a little apart from the text above. */
@Composable
fun HeroActions(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.padding(top = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), content = content)
}

/** The hero's main action, in sun yellow: the one warm thing on the screen. */
@Composable
fun HeroButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptics = rememberHaptics()
    val colors = MaterialTheme.flockrColors
    Button(
        onClick = { haptics.tap(); onClick() },
        modifier = modifier,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(containerColor = colors.sun, contentColor = colors.onSun),
    ) { Text(text, style = MaterialTheme.typography.labelLargeEmphasized) }
}

/** A secondary hero action, a translucent pill that reads as part of the cobalt. */
@Composable
fun HeroSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val colors = MaterialTheme.flockrColors
    Button(
        onClick = { haptics.tap(); onClick() },
        modifier = modifier,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(containerColor = colors.onHero.copy(alpha = HeroTonalAlpha), contentColor = colors.onHero),
    ) { Text(text, style = MaterialTheme.typography.labelLargeEmphasized) }
}

/**
 * Turns the status bar icons light while the calling screen is shown, since dark icons on the hero
 * cobalt are unreadable in both themes, and puts back what was there when it leaves.
 */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

private val HeroCorner = Spacing.xxxl
private const val HeroTonalAlpha = 0.16f
private const val FlockAlpha = 0.05f
private const val PhotoWashTop = 0.55f
private const val PhotoWashBottom = 0.9f
private const val MapWashTop = 0.3f
private const val CountUpMillis = 700
private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
