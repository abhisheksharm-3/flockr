/** Loading placeholders in the shape of what is coming, so a screen arrives laid out instead of as a spinner. */
package `in`.xroden.flockr.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors

private const val SHIMMER_MILLIS = 1300
private const val HERO_BAR_ALPHA = 0.18f
private const val HERO_GLOW_ALPHA = 0.32f
private val LineHeight = 14.dp
private val TitleLineHeight = 20.dp
private val HeroFigureHeight = 48.dp
private val PillHeight = 44.dp

/**
 * A bar of [base] with a soft highlight of [glow] sweeping across it. Every placeholder on screen
 * shares one sweep, so they shimmer together rather than each at its own pace. With the system's
 * animations off the bar stays plain.
 */
@Composable
private fun Modifier.shimmer(base: Color, glow: Color, shape: Shape): Modifier {
    val context = LocalContext.current
    val isMotionOff = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val sweep = if (isMotionOff) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
        value
    }
    return clip(shape).background(base).drawWithContent {
        drawContent()
        if (!isMotionOff) {
            val span = size.width * 2
            val x = -size.width + span * sweep
            drawRect(Brush.linearGradient(listOf(Color.Transparent, glow, Color.Transparent), start = Offset(x, 0f), end = Offset(x + size.width, size.height)))
        }
    }
}

@Composable
private fun Bar(width: Dp?, height: Dp, onHero: Boolean = false, shape: Shape = RoundedCornerShape(height / 2)) {
    val base = if (onHero) MaterialTheme.flockrColors.onHero.copy(alpha = HERO_BAR_ALPHA) else MaterialTheme.colorScheme.surfaceContainerHigh
    val glow = if (onHero) MaterialTheme.flockrColors.onHero.copy(alpha = HERO_GLOW_ALPHA) else MaterialTheme.colorScheme.surfaceContainerHighest
    Box((if (width == null) Modifier.fillMaxWidth() else Modifier.width(width)).height(height).shimmer(base, glow, shape))
}

/** One list row's worth: a circle and two lines, with an amount at the end. */
@Composable
fun SkeletonRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Bar(ComponentHeight.avatar, ComponentHeight.avatar, shape = CircleShape)
        Column(Modifier.weight(1f).padding(top = Spacing.xxs), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Bar(160.dp, LineHeight)
            Bar(100.dp, LineHeight)
        }
        Bar(64.dp, LineHeight)
    }
}

/** A section's worth of placeholder rows under a heading placeholder, for a list below a real title bar. */
@Composable
fun SkeletonRows(count: Int = 7) {
    Column(Modifier.padding(top = Spacing.lg)) {
        Box(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) { Bar(90.dp, LineHeight) }
        repeat(count) { SkeletonRow() }
    }
}

/** A screen that opens with a cobalt header: the header's label and figure, then rows. */
@Composable
fun SkeletonHeroScreen(rows: Int = 6) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics { contentDescription = "Loading" }) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = Spacing.xxxl, bottomEnd = Spacing.xxxl))) {
            HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
            Column(Modifier.statusBarsPadding().padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Bar(140.dp, TitleLineHeight, onHero = true)
                Bar(200.dp, HeroFigureHeight, onHero = true)
                Bar(220.dp, LineHeight, onHero = true)
                Row(Modifier.padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Bar(110.dp, PillHeight, onHero = true)
                    Bar(110.dp, PillHeight, onHero = true)
                }
            }
        }
        SkeletonRows(rows)
    }
}

/** A screen with a plain title: the title, then rows. */
@Composable
fun SkeletonListScreen(rows: Int = 7) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().semantics { contentDescription = "Loading" }) {
        Box(Modifier.padding(start = Spacing.lg, top = Spacing.xxl)) { Bar(180.dp, HeroFigureHeight - Spacing.md) }
        SkeletonRows(rows)
    }
}

/** A sentence form: the cobalt header with its big value, then a few tokens. */
@Composable
fun SkeletonFormScreen() {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).semantics { contentDescription = "Loading" },
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = Spacing.xxxl, bottomEnd = Spacing.xxxl))) {
            HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
            Column(Modifier.statusBarsPadding().padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Bar(110.dp, TitleLineHeight, onHero = true)
                Bar(180.dp, HeroFigureHeight + Spacing.lg, onHero = true)
                Bar(240.dp, TitleLineHeight + Spacing.sm, onHero = true)
            }
        }
        Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { Bar(70.dp, PillHeight); Bar(90.dp, PillHeight); Bar(110.dp, PillHeight) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { Bar(130.dp, PillHeight); Bar(150.dp, PillHeight) }
        }
    }
}

/**
 * A house's hub while it loads, in the hub's own shape: the house's cobalt filling the screen with
 * its name, faces and a prompt, and the drawer resting over the bottom with its pills and rows.
 */
@Composable
fun SkeletonHubScreen() {
    Box(Modifier.fillMaxSize().semantics { contentDescription = "Loading" }) {
        HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
        Column(Modifier.statusBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.xxxxl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Bar(220.dp, HeroFigureHeight, onHero = true)
            Bar(160.dp, LineHeight, onHero = true)
            Row(Modifier.padding(vertical = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                repeat(3) { Bar(ComponentHeight.avatarLarge, ComponentHeight.avatarLarge, onHero = true, shape = CircleShape) }
            }
            Bar(90.dp, LineHeight, onHero = true)
            Bar(260.dp, TitleLineHeight + Spacing.sm, onHero = true)
            Row(Modifier.padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Bar(120.dp, PillHeight, onHero = true)
                Bar(110.dp, PillHeight, onHero = true)
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Spacing.xxxl, topEnd = Spacing.xxxl))
                .background(MaterialTheme.colorScheme.background)
                .padding(top = Spacing.xl),
        ) {
            Row(Modifier.padding(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Bar(110.dp, PillHeight); Bar(100.dp, PillHeight); Bar(120.dp, PillHeight)
            }
            SkeletonRows(3)
        }
    }
}
