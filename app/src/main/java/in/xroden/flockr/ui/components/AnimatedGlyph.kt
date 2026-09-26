/** Icons that move once when something happens, the Compose counterpart of animated icon sets on the web. */
package `in`.xroden.flockr.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

/** How a glyph moves when its trigger changes. Each suits a meaning, not a mood. */
enum class GlyphMotion {
    /** A quick side-to-side ring, for something that wants attention: a bell with news. */
    WIGGLE,

    /** A small swell and settle, for something just made or completed: an added item, a done chore. */
    POP,

    /** A half turn, for something that swaps or refreshes. */
    SPIN,

    /** A short hop forward and back, for something sent or moved on. */
    NUDGE,
}

private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

/**
 * [icon] that plays [motion] each time [trigger] changes after it first appears, so a screen opening
 * doesn't set every glyph moving at once. With the system's animations switched off it stays still.
 * Decorative unless [contentDescription] is given, like [Icon].
 */
@Composable
fun AnimatedGlyph(
    icon: ImageVector,
    trigger: Any?,
    motion: GlyphMotion,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    val context = LocalContext.current
    val isMotionOff = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val progress = remember { Animatable(0f) }
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (!hasAppeared) { hasAppeared = true; return@LaunchedEffect }
        if (isMotionOff) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, if (motion == GlyphMotion.WIGGLE) tween(WIGGLE_MILLIS, easing = LinearEasing) else tween(MOVE_MILLIS, easing = EaseOutQuart))
    }
    Icon(
        icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.graphicsLayer {
            val t = progress.value
            when (motion) {
                GlyphMotion.WIGGLE -> rotationZ = if (t in 0f..<1f) WIGGLE_DEGREES * kotlin.math.sin(t * WIGGLE_TURNS) * (1f - t) else 0f
                GlyphMotion.POP -> (1f + POP_GROWTH * kotlin.math.sin(t * Math.PI.toFloat())).let { scaleX = it; scaleY = it }
                GlyphMotion.SPIN -> rotationZ = SPIN_DEGREES * t
                GlyphMotion.NUDGE -> translationX = size.width * NUDGE_REACH * kotlin.math.sin(t * Math.PI.toFloat())
            }
        },
    )
}

private const val WIGGLE_MILLIS = 520
private const val MOVE_MILLIS = 360
private const val WIGGLE_DEGREES = 18f
private const val WIGGLE_TURNS = 6f * Math.PI.toFloat()
private const val POP_GROWTH = 0.28f
private const val SPIN_DEGREES = 180f
private const val NUDGE_REACH = 0.35f
