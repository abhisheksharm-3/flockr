package `in`.xroden.flockr.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Named access to the theme's [androidx.compose.material3.MotionScheme].
 *
 * Material 3 Expressive splits motion in two. Spatial specs move things — position, size, shape —
 * and are springy, so a card that slides in overshoots slightly before settling. Effects specs
 * change properties that have no physical analogue — colour, alpha, elevation — and stay
 * monotonic, because a colour that overshoots reads as a glitch rather than as motion.
 *
 * Reach for these instead of hand-written `tween`/`spring` values. They come from the theme, so
 * switching [androidx.compose.material3.MotionScheme.standard] for
 * [androidx.compose.material3.MotionScheme.expressive] restyles every animation in the app at once.
 */
object Motion {

    /** Springy. Use for anything that moves, resizes, or reshapes. */
    val spatial: FiniteAnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.defaultSpatialSpec()

    /** Springy and quick. Use for small elements and immediate touch response. */
    val spatialFast: FiniteAnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.fastSpatialSpec()

    /** Springy and deliberate. Use for large surfaces and full-screen transitions. */
    val spatialSlow: FiniteAnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.slowSpatialSpec()

    /** Monotonic. Use for colour, alpha, and elevation. */
    val effects: FiniteAnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.defaultEffectsSpec()

    /** Monotonic and quick. Use for hover, focus, and press feedback. */
    val effectsFast: FiniteAnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.fastEffectsSpec()

    /** Monotonic and deliberate. Use for scrims and background washes. */
    val effectsSlow: FiniteAnimationSpec<Float>
        @Composable @ReadOnlyComposable get() = MaterialTheme.motionScheme.slowEffectsSpec()
}

/**
 * Typed spatial spec for animating something other than a Float — a Dp, a Color, an Offset.
 *
 * `animateDpAsState(target, animationSpec = spatialSpec())`
 */
@Composable
@ReadOnlyComposable
fun <T> spatialSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

/** Typed fast spatial spec. See [spatialSpec]. */
@Composable
@ReadOnlyComposable
fun <T> spatialFastSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastSpatialSpec()

/** Typed slow spatial spec. See [spatialSpec]. */
@Composable
@ReadOnlyComposable
fun <T> spatialSlowSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.slowSpatialSpec()

/** Typed effects spec, for colour, alpha and elevation. See [spatialSpec]. */
@Composable
@ReadOnlyComposable
fun <T> effectsSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultEffectsSpec()

/** Typed fast effects spec. See [spatialSpec]. */
@Composable
@ReadOnlyComposable
fun <T> effectsFastSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastEffectsSpec()
