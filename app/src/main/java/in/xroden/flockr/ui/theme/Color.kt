/** Flockr's cobalt palette for both themes, set in OKLCH and converted to sRGB; every text pairing meets WCAG AA. */
package `in`.xroden.flockr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colours Material's scheme has no slot for.
 *
 * [hero] is the cobalt a screen's headline block is drenched in, with [onHero] for text on it and
 * [onHeroVariant] for its quieter lines. [sun] is the one warm colour, kept for the main action on a
 * hero. [positive] and [negative] colour money you're owed and money you owe; a sign or a word always
 * carries the same meaning, so the colour is never the only cue.
 */
@Immutable
data class FlockrColors(
    val hero: Color,
    val onHero: Color,
    val onHeroVariant: Color,
    val sun: Color,
    val onSun: Color,
    val positive: Color,
    val negative: Color,
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1957D2),
    onPrimary = Color(0xFFF7FAFF),
    primaryContainer = Color(0xFFD4E5FF),
    onPrimaryContainer = Color(0xFF032A78),
    secondary = Color(0xFF435577),
    onSecondary = Color(0xFFF7FAFF),
    secondaryContainer = Color(0xFFDFE8F9),
    onSecondaryContainer = Color(0xFF1D2D4C),
    tertiary = Color(0xFF0B764D),
    onTertiary = Color(0xFFF6FCF8),
    tertiaryContainer = Color(0xFFCCF3DD),
    onTertiaryContainer = Color(0xFF033D26),
    error = Color(0xFFC2272D),
    onError = Color(0xFFFFF8F7),
    errorContainer = Color(0xFFFFDEDB),
    onErrorContainer = Color(0xFF6F191A),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF121A2B),
    surface = Color(0xFFF4F7FB),
    onSurface = Color(0xFF121A2B),
    surfaceVariant = Color(0xFFE3E8F0),
    onSurfaceVariant = Color(0xFF525B6C),
    surfaceContainerLowest = Color(0xFFFCFDFF),
    surfaceContainerLow = Color(0xFFFAFCFF),
    surfaceContainer = Color(0xFFEEF2F8),
    surfaceContainerHigh = Color(0xFFE9EDF4),
    surfaceContainerHighest = Color(0xFFE1E6EF),
    outline = Color(0xFF9EA5B2),
    outlineVariant = Color(0xFFDADEE6),
    inverseSurface = Color(0xFF1D2638),
    inverseOnSurface = Color(0xFFEBEFF5),
    inversePrimary = Color(0xFF91B7FE),
    scrim = Color(0x99060A12),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BBEFF),
    onPrimary = Color(0xFF04215B),
    primaryContainer = Color(0xFF153A83),
    onPrimaryContainer = Color(0xFFD6E5FF),
    secondary = Color(0xFFB0BED8),
    onSecondary = Color(0xFF1C263A),
    secondaryContainer = Color(0xFF283347),
    onSecondaryContainer = Color(0xFFD5DFEF),
    tertiary = Color(0xFF82D2A8),
    onTertiary = Color(0xFF0B2E1E),
    tertiaryContainer = Color(0xFF16412D),
    onTertiaryContainer = Color(0xFFC2E9D3),
    error = Color(0xFFF98F87),
    onError = Color(0xFF551112),
    errorContainer = Color(0xFF682321),
    onErrorContainer = Color(0xFFFEDBD7),
    background = Color(0xFF0F141D),
    onBackground = Color(0xFFEBEFF5),
    surface = Color(0xFF0F141D),
    onSurface = Color(0xFFEBEFF5),
    surfaceVariant = Color(0xFF202733),
    onSurfaceVariant = Color(0xFFA2ABBB),
    surfaceContainerLowest = Color(0xFF090D15),
    surfaceContainerLow = Color(0xFF141A24),
    surfaceContainer = Color(0xFF181E2A),
    surfaceContainerHigh = Color(0xFF1F2733),
    surfaceContainerHighest = Color(0xFF29313F),
    outline = Color(0xFF5C6472),
    outlineVariant = Color(0xFF29313D),
    inverseSurface = Color(0xFFE4E8EF),
    inverseOnSurface = Color(0xFF1A2230),
    inversePrimary = Color(0xFF1957D2),
    scrim = Color(0x99060A12),
)

val LightFlockrColors = FlockrColors(
    hero = Color(0xFF1957D2),
    onHero = Color(0xFFF7FAFF),
    onHeroVariant = Color(0xFFD8E5FD),
    sun = Color(0xFFFDDC5B),
    onSun = Color(0xFF142139),
    positive = Color(0xFF0B764D),
    negative = Color(0xFFC2272D),
)

val DarkFlockrColors = FlockrColors(
    hero = Color(0xFF093DA1),
    onHero = Color(0xFFF1F5FC),
    onHeroVariant = Color(0xFFB9CBEC),
    sun = Color(0xFFF4D660),
    onSun = Color(0xFF0D1A32),
    positive = Color(0xFF82D2A8),
    negative = Color(0xFFF98F87),
)

internal val LocalFlockrColors = staticCompositionLocalOf { LightFlockrColors }

/** The current theme's [FlockrColors], read like `MaterialTheme.flockrColors.hero`. */
val MaterialTheme.flockrColors: FlockrColors
    @Composable @ReadOnlyComposable get() = LocalFlockrColors.current
