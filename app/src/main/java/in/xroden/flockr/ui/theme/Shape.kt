package `in`.xroden.flockr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// MODERN SHAPES
// Smooth, rounded corners for a friendly, contemporary aesthetic
// =============================================================================

val Shapes = Shapes(
    extraSmall = MaterialTheme.shapes.extraSmall,   // Soft for small elements like chips
    small = MaterialTheme.shapes.medium,       // Rounded for tags and small cards
    medium = MaterialTheme.shapes.large,      // Smooth for buttons and inputs
    large = MaterialTheme.shapes.large,       // Cards with elegant curves
    extraLarge = MaterialTheme.shapes.extraLarge   // Dialogs, sheets, and modals
)

