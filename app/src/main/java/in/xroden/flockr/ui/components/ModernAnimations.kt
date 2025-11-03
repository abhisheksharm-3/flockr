package `in`.xroden.flockr.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

// =============================================================================
// MODERN ANIMATIONS & MICRO-INTERACTIONS
// Spring-based animations for natural, responsive feel
// =============================================================================

/**
 * Press animation modifier - scales element on press
 */
fun Modifier.pressAnimation(
    targetScale: Float = 0.96f,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessLow
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        ),
        label = "press_scale"
    )
    
    this.scale(scale)
}

/**
 * Fade in animation for list items
 */
@Composable
fun FadeInListItem(
    visible: Boolean = true,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { it / 4 }
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 200,
                easing = LinearOutSlowInEasing
            )
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 200,
                easing = LinearOutSlowInEasing
            ),
            targetOffsetY = { -it / 4 }
        )
    ) {
        content()
    }
}

/**
 * Shimmer loading animation
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    this.graphicsLayer {
        this.alpha = alpha
    }
}

/**
 * Bounce animation for emphasis
 */
fun Modifier.bounceAnimation(trigger: Boolean): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (trigger) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )
    
    this.scale(scale)
}

/**
 * Rotation animation for loading indicators
 */
@Composable
fun rememberRotationAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_degrees"
    )
    return rotation
}

/**
 * Slide in from side animation
 */
@Composable
fun SlideInFromRight(
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            initialOffsetX = { it }
        ) + fadeIn(
            animationSpec = tween(300)
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 250,
                easing = LinearOutSlowInEasing
            ),
            targetOffsetX = { it }
        ) + fadeOut(
            animationSpec = tween(250)
        )
    ) {
        content()
    }
}

/**
 * Expand/collapse animation
 */
@Composable
fun ExpandableContent(
    expanded: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut()
    ) {
        content()
    }
}

