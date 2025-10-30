package `in`.xroden.flockr.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale

// Animated list item entry
@Composable
fun AnimatedListItem(
    visible: Boolean = true,
    index: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 50
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = index * 50
            )
        ),
        exit = fadeOut() + slideOutVertically()
    ) {
        content()
    }
}

// Pulse animation for important items
@Composable
fun PulseAnimation(
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.scale(scale)) {
        content()
    }
}

// Fade in content
@Composable
fun FadeInContent(
    visible: Boolean = true,
    durationMillis: Int = 300,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis)),
        exit = fadeOut(animationSpec = tween(durationMillis))
    ) {
        content()
    }
}

// Slide in from bottom
@Composable
fun SlideInFromBottom(
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it }
        ) + fadeOut()
    ) {
        content()
    }
}

// Expandable content
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
        exit = shrinkVertically() + fadeOut()
    ) {
        content()
    }
}

