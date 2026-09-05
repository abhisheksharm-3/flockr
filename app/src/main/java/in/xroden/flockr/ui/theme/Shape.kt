package `in`.xroden.flockr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The Expressive shape scale. Material 3 Expressive widened the scale from five slots to eight,
 * adding the two "increased" steps and extraExtraLarge so a layout can step between roundness
 * levels without jumping.
 *
 * Rounder than the old scale on purpose: Expressive leans on shape, not borders, to separate
 * surfaces.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    largeIncreased = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

/** A fully rounded shape, for pills, avatars and toggle groups. */
val PillShape = RoundedCornerShape(percent = 50)
