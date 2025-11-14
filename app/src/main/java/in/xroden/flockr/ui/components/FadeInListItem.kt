package `in`.xroden.flockr.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * A composable that wraps content with a fade-in and slide-up animation
 */
@Composable
fun FadeInListItem(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 300),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}

