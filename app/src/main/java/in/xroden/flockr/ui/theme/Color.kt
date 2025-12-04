package `in`.xroden.flockr.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// MATERIAL 3 COLOR SCHEMES
// Clean, semantic color definitions using Material Design 3 guidelines
// =============================================================================

// Light Theme Color Scheme
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),              // Indigo 600 (More vibrant than Blue 600)
    onPrimary = Color(0xFFFFFFFF),            // White
    primaryContainer = Color(0xFFE0E7FF),     // Indigo 100
    onPrimaryContainer = Color(0xFF312E81),   // Indigo 900

    secondary = Color(0xFFEC4899),            // Pink 500 (Vibrant accent)
    onSecondary = Color(0xFFFFFFFF),          // White
    secondaryContainer = Color(0xFFFCE7F3),   // Pink 100
    onSecondaryContainer = Color(0xFF831843), // Pink 900

    tertiary = Color(0xFF0EA5E9),             // Sky 500 (Playful tertiary)
    onTertiary = Color(0xFFFFFFFF),           // White
    tertiaryContainer = Color(0xFFE0F2FE),    // Sky 100
    onTertiaryContainer = Color(0xFF0C4A6E),  // Sky 900

    error = Color(0xFFEF4444),                // Red 500
    onError = Color(0xFFFFFFFF),              // White
    errorContainer = Color(0xFFFEE2E2),       // Red 100
    onErrorContainer = Color(0xFF7F1D1D),     // Red 900

    background = Color(0xFFF8FAFC),           // Slate 50
    onBackground = Color(0xFF0F172A),         // Slate 900
    surface = Color(0xFFFFFFFF),              // White
    onSurface = Color(0xFF0F172A),            // Slate 900
    surfaceVariant = Color(0xFFF1F5F9),       // Slate 100
    onSurfaceVariant = Color(0xFF64748B),     // Slate 500
    outline = Color(0xFFCBD5E1),              // Slate 300
    outlineVariant = Color(0xFFE2E8F0),       // Slate 200

    scrim = Color(0x80000000)
)

// Dark Theme Color Scheme
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),              // Indigo 400
    onPrimary = Color(0xFFFFFFFF),            // White
    primaryContainer = Color(0xFF3730A3),     // Indigo 800
    onPrimaryContainer = Color(0xFFE0E7FF),   // Indigo 100

    secondary = Color(0xFFF472B6),            // Pink 400
    onSecondary = Color(0xFFFFFFFF),          // White
    secondaryContainer = Color(0xFF9D174D),   // Pink 800
    onSecondaryContainer = Color(0xFFFCE7F3), // Pink 100

    tertiary = Color(0xFF38BDF8),             // Sky 400
    onTertiary = Color(0xFFFFFFFF),           // White
    tertiaryContainer = Color(0xFF075985),    // Sky 800
    onTertiaryContainer = Color(0xFFE0F2FE),  // Sky 100

    error = Color(0xFFF87171),                // Red 400
    onError = Color(0xFFFFFFFF),              // White
    errorContainer = Color(0xFF7F1D1D),       // Red 900
    onErrorContainer = Color(0xFFFEE2E2),     // Red 100

    background = Color(0xFF0F172A),           // Slate 900
    onBackground = Color(0xFFF8FAFC),         // Slate 50
    surface = Color(0xFF1E293B),              // Slate 800
    onSurface = Color(0xFFF8FAFC),            // Slate 50
    surfaceVariant = Color(0xFF334155),       // Slate 700
    onSurfaceVariant = Color(0xFF94A3B8),     // Slate 400
    outline = Color(0xFF475569),              // Slate 600
    outlineVariant = Color(0xFF334155),       // Slate 700

    scrim = Color(0x80000000)
)

// =============================================================================
// CATEGORY COLORS
// Used for expense categories, charts, and visual categorization
// =============================================================================

val CategoryGreen = Color(0xFF4CAF50)      // Groceries, Food
val CategoryBlue = Color(0xFF2196F3)       // Utilities, Services
val CategoryPurple = Color(0xFF9C27B0)     // Entertainment
val CategoryYellow = Color(0xFFFFC107)     // Transport
val CategoryPink = Color(0xFFE91E63)       // Shopping
val CategoryOrange = Color(0xFFFF5722)     // Rent, Housing
val CategoryTeal = Color(0xFF009688)       // Healthcare
val CategoryIndigo = Color(0xFF3F51B5)     // Education
val CategoryRed = Color(0xFFEF4444)        // Negative/Error
