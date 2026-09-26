package `in`.xroden.flockr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The Expressive shape scale, eight steps so a layout can move between roundness levels without
 * jumping. Fields and small controls sit at [Shapes.medium]; grouped surfaces at [Shapes.large] and
 * up; the hero's bottom edge at [Shapes.extraExtraLarge].
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(36.dp),
)
