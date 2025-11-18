package `in`.xroden.flockr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// MODERN SHAPES
// Smooth, rounded corners for a friendly, contemporary aesthetic
// =============================================================================

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Soft for small elements like chips
    small = RoundedCornerShape(8.dp),       // Rounded for tags and small cards
    medium = RoundedCornerShape(12.dp),      // Smooth for buttons and inputs
    large = RoundedCornerShape(16.dp),       // Cards with elegant curves
    extraLarge = RoundedCornerShape(28.dp)   // Dialogs, sheets, and modals
)

