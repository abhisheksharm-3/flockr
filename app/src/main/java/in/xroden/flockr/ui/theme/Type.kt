package `in`.xroden.flockr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import `in`.xroden.flockr.R

// =============================================================================
// MODERN FONT FAMILIES - Google Fonts
// Space Grotesk for headings (geometric, modern, tech-forward)
// Plus Jakarta Sans for body (rounded, friendly, highly legible)
// =============================================================================

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Space Grotesk: Modern, geometric sans-serif for headings and display text
private val spaceGroteskFont = GoogleFont("Space Grotesk")
val SpaceGroteskFontFamily = FontFamily(
    Font(googleFont = spaceGroteskFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = spaceGroteskFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = spaceGroteskFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = spaceGroteskFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Plus Jakarta Sans: Rounded, friendly sans-serif for body and UI elements
private val plusJakartaSansFont = GoogleFont("Plus Jakarta Sans")
val PlusJakartaSansFontFamily = FontFamily(
    Font(googleFont = plusJakartaSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = plusJakartaSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = plusJakartaSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = plusJakartaSansFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Material 3 Typography with modern Google Fonts
val AppTypography = Typography(
    // Display styles - Large, bold headlines (Space Grotesk)
    displayLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles - Section headers (Space Grotesk)
    headlineLarge = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles - Card titles and prominent UI text (Plus Jakarta Sans)
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body styles - Content text (Plus Jakarta Sans)
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    
    // Label styles - Buttons, tags, and small UI elements (Plus Jakarta Sans)
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)

// =============================================================================
// UTILITY TEXT STYLES - For data-rich, financial UI
// =============================================================================

// Numeric Display Styles - For financial amounts, counts, stats
val NumericDisplayLarge = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 48.sp,
    lineHeight = 56.sp,
    letterSpacing = (-0.5).sp
)

val NumericDisplayMedium = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 44.sp,
    letterSpacing = 0.sp
)

val NumericDisplaySmall = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = 0.sp
)

val NumericBody = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

// Label Styles - For data labels, captions, overlines
val OverlineLabel = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.sp  // Wide tracking for uppercase labels
)

val DataLabel = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp
)

val CaptionText = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.2.sp
)

// Card/Section Styles
val CardTitle = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp
)

val CardSubtitle = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

// List Item Styles
val ListItemTitle = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.sp
)

val ListItemSubtext = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp
)
