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
    primary = Color(0xFF3B82F6),              // Bright blue
    onPrimary = Color(0xFFFFFFFF),            // White
    primaryContainer = Color(0xFFDCEAFB),     // Very light blue
    onPrimaryContainer = Color(0xFF0F172A),   // Deep slate

    secondary = Color(0xFF6366F1),            // Indigo
    onSecondary = Color(0xFFFFFFFF),          // White
    secondaryContainer = Color(0xFFE0E7FF),   // Very light indigo
    onSecondaryContainer = Color(0xFF312E81), // Deep indigo

    tertiary = Color(0xFFEC4899),             // Pink
    onTertiary = Color(0xFFFFFFFF),           // White
    tertiaryContainer = Color(0xFFFCE7F3),    // Very light pink
    onTertiaryContainer = Color(0xFF831843),  // Deep pink

    error = Color(0xFFEF4444),                // Red
    onError = Color(0xFFFFFFFF),              // White
    errorContainer = Color(0xFFFEE2E2),       // Light red
    onErrorContainer = Color(0xFFDC2626),     // Dark red

    background = Color(0xFFF8FAFC),           // Off-white
    onBackground = Color(0xFF0F172A),         // Deep slate
    surface = Color(0xFFFFFFFF),              // Pure white
    onSurface = Color(0xFF0F172A),            // Deep slate
    surfaceVariant = Color(0xFFF1F5F9),       // Light gray
    onSurfaceVariant = Color(0xFF475569),     // Body text
    outline = Color(0xFFCBD5E1),              // Dividers
    outlineVariant = Color(0xFFE2E8F0),       // Subtle borders

    scrim = Color(0x80000000)                 // Semi-transparent black
)

// Dark Theme Color Scheme
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),              // Bright blue
    onPrimary = Color(0xFF020617),            // Deepest dark
    primaryContainer = Color(0xFF1E40AF),     // Deep blue
    onPrimaryContainer = Color(0xFFDCEAFB),   // Light blue

    secondary = Color(0xFF818CF8),            // Light purple
    onSecondary = Color(0xFF020617),          // Deepest dark
    secondaryContainer = Color(0xFF4F46E5),   // Dark purple
    onSecondaryContainer = Color(0xFFE0E7FF), // Very light indigo

    tertiary = Color(0xFFF472B6),             // Light pink
    onTertiary = Color(0xFF020617),           // Deepest dark
    tertiaryContainer = Color(0xFF9F1239),    // Dark pink
    onTertiaryContainer = Color(0xFFFCE7F3),  // Very light pink

    error = Color(0xFFFCA5A5),                // Light red
    onError = Color(0xFF020617),              // Deepest dark
    errorContainer = Color(0xFFDC2626),       // Dark red
    onErrorContainer = Color(0xFFFEE2E2),     // Light red

    background = Color(0xFF020617),           // Deepest background
    onBackground = Color(0xFFE2E8F0),         // Light text
    surface = Color(0xFF0F172A),              // Dark surface
    onSurface = Color(0xFFE2E8F0),            // Light text
    surfaceVariant = Color(0xFF1E293B),       // Elevated surface
    onSurfaceVariant = Color(0xFF94A3B8),     // Secondary text
    outline = Color(0xFF475569),              // Borders
    outlineVariant = Color(0xFF334155),       // Subtle borders

    scrim = Color(0x80000000)                 // Semi-transparent black
)
