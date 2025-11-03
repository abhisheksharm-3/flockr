package `in`.xroden.flockr.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

// =============================================================================
// MODERN VISUAL EFFECTS
// Gradient backgrounds and modern visual effects for contemporary UI
// =============================================================================

/**
 * Adds a subtle gradient background to the composable
 */
fun Modifier.gradientBackground(
    startColor: Color? = null,
    endColor: Color? = null,
    angle: Float = 135f
): Modifier = composed {
    val start = startColor ?: MaterialTheme.colorScheme.primaryContainer
    val end = endColor ?: MaterialTheme.colorScheme.secondaryContainer
    
    this.drawBehind {
        val angleRad = Math.toRadians(angle.toDouble()).toFloat()
        val diagonal = size.width + size.height
        val endX = diagonal * kotlin.math.cos(angleRad)
        val endY = diagonal * kotlin.math.sin(angleRad)
        
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(start, end),
                start = Offset.Zero,
                end = Offset(endX, endY)
            )
        )
    }
}

/**
 * Creates a vibrant purple to pink gradient
 */
fun Modifier.purplePinkGradient(): Modifier = composed {
    this.drawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    CategoryPurple.copy(alpha = 0.1f),
                    CategoryPink.copy(alpha = 0.1f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
    }
}

/**
 * Creates a teal to blue gradient
 */
fun Modifier.tealBlueGradient(): Modifier = composed {
    this.drawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    CategoryTeal.copy(alpha = 0.1f),
                    InfoBlue.copy(alpha = 0.1f)
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
    }
}

/**
 * Creates a glass morphism effect with blur-like appearance
 * Note: Actual blur requires API 31+, this creates a similar visual effect
 */
fun Modifier.glassMorphism(): Modifier = composed {
    val surfaceColor = MaterialTheme.colorScheme.surface
    this.drawBehind {
        drawRect(
            color = surfaceColor.copy(alpha = 0.7f)
        )
    }
}

