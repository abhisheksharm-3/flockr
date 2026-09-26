/** How screens move as the app navigates: Material's shared X axis, tuned to Expressive easing. */
package `in`.xroden.flockr.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

private const val TRAVEL_FRACTION = 10
private const val MOVE_MILLIS = 350
private const val FADE_IN_MILLIS = 210
private const val FADE_IN_DELAY_MILLIS = 60
private const val FADE_OUT_MILLIS = 90

/** Material's emphasized decelerate curve: fast off the mark, long gentle landing. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

/**
 * The shared X axis: the incoming screen slides a tenth of the width in the direction of travel
 * while it fades in, and the outgoing one slides the same way while it fades out quickly, so the
 * two never read as overlapping. Going back mirrors it; with predictive back the pop runs under the
 * finger. The short travel keeps every frame cheap, which is what the full-width slide was not.
 */
internal val SharedAxisEnter: Enter = { slideIn(forward = true) }
internal val SharedAxisExit: Exit = { slideOut(forward = true) }
internal val SharedAxisPopEnter: Enter = { slideIn(forward = false) }
internal val SharedAxisPopExit: Exit = { slideOut(forward = false) }

private fun slideIn(forward: Boolean): EnterTransition =
    slideInHorizontally(tween(MOVE_MILLIS, easing = EmphasizedDecelerate)) { width -> (if (forward) width else -width) / TRAVEL_FRACTION } +
        fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_IN_DELAY_MILLIS))

private fun slideOut(forward: Boolean): ExitTransition =
    slideOutHorizontally(tween(MOVE_MILLIS, easing = EmphasizedDecelerate)) { width -> (if (forward) -width else width) / TRAVEL_FRACTION } +
        fadeOut(tween(FADE_OUT_MILLIS))
