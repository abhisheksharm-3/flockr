package `in`.xroden.flockr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Modern rounded shapes inspired by the design samples
val Shapes = Shapes(
    // Small components (buttons, chips, small cards)
    small = RoundedCornerShape(12.dp),

    // Medium components (cards, dialogs, bottom sheets)
    medium = RoundedCornerShape(16.dp),

    // Large components (modal sheets, large cards)
    large = RoundedCornerShape(24.dp),

    // Extra large (full screen sheets)
    extraLarge = RoundedCornerShape(28.dp)
)

