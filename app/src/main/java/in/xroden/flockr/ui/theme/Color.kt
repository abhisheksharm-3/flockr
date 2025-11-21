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
    primary = Color(0xFF2563EB),              // Blue 600
    onPrimary = Color(0xFFFFFFFF),            // White
    primaryContainer = Color(0xFFDBEAFE),     // Blue 100
    onPrimaryContainer = Color(0xFF1E3A8A),   // Blue 900

    secondary = Color(0xFF4F46E5),            // Indigo 600
    onSecondary = Color(0xFFFFFFFF),          // White
    secondaryContainer = Color(0xFFE0E7FF),   // Indigo 100
    onSecondaryContainer = Color(0xFF312E81), // Indigo 900

    tertiary = Color(0xFFDB2777),             // Pink 600
    onTertiary = Color(0xFFFFFFFF),           // White
    tertiaryContainer = Color(0xFFFCE7F3),    // Pink 100
    onTertiaryContainer = Color(0xFF831843),  // Pink 900

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
    primary = Color(0xFF3B82F6),              // Blue 500
    onPrimary = Color(0xFFFFFFFF),            // White
    primaryContainer = Color(0xFF1E40AF),     // Blue 800
    onPrimaryContainer = Color(0xFFDBEAFE),   // Blue 100

    secondary = Color(0xFF6366F1),            // Indigo 500
    onSecondary = Color(0xFFFFFFFF),          // White
    secondaryContainer = Color(0xFF4338CA),   // Indigo 700
    onSecondaryContainer = Color(0xFFE0E7FF), // Indigo 100

    tertiary = Color(0xFFEC4899),             // Pink 500
    onTertiary = Color(0xFFFFFFFF),           // White
    tertiaryContainer = Color(0xFFBE185D),    // Pink 700
    onTertiaryContainer = Color(0xFFFCE7F3),  // Pink 100

    error = Color(0xFFEF4444),                // Red 500
    onError = Color(0xFFFFFFFF),              // White
    errorContainer = Color(0xFF991B1B),       // Red 800
    onErrorContainer = Color(0xFFFEE2E2),     // Red 100

    background = Color(0xFF0B1121),           // Deep Slate (Darker)
    onBackground = Color(0xFFF8FAFC),         // Slate 50
    surface = Color(0xFF151E32),              // Lighter Slate
    onSurface = Color(0xFFF8FAFC),            // Slate 50
    surfaceVariant = Color(0xFF1E293B),       // Slate 800
    onSurfaceVariant = Color(0xFF94A3B8),     // Slate 400
    outline = Color(0xFF334155),              // Slate 700
    outlineVariant = Color(0xFF1E293B),       // Slate 800

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
