/** Flockr's type: Figtree throughout, bundled so it never falls back, with digits that line up. */
package `in`.xroden.flockr.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import `in`.xroden.flockr.R

private fun figtree(weight: FontWeight) =
    Font(R.font.figtree, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** One variable file carries every weight the scale uses. */
val FigtreeFontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black).map(::figtree),
)

/**
 * Every style uses tabular figures, so amounts stacked in a list line up digit for digit and a
 * number that changes never shifts its neighbours.
 */
private fun style(size: Int, line: Int, weight: FontWeight, tracking: TextUnit = 0.em) = TextStyle(
    fontFamily = FigtreeFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking,
    fontFeatureSettings = "tnum",
)

private val base = Typography(
    displayLarge = style(56, 60, FontWeight.ExtraBold, (-0.03).em),
    displayMedium = style(46, 52, FontWeight.ExtraBold, (-0.03).em),
    displaySmall = style(38, 44, FontWeight.ExtraBold, (-0.025).em),
    headlineLarge = style(32, 38, FontWeight.ExtraBold, (-0.02).em),
    headlineMedium = style(27, 32, FontWeight.ExtraBold, (-0.02).em),
    headlineSmall = style(23, 28, FontWeight.Bold, (-0.015).em),
    titleLarge = style(20, 26, FontWeight.Bold, (-0.01).em),
    titleMedium = style(17, 24, FontWeight.SemiBold),
    titleSmall = style(15, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(13, 18, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.SemiBold),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.01.em),
    labelSmall = style(11, 14, FontWeight.SemiBold, 0.04.em),
)

/**
 * Material 3 Expressive pairs every slot with an "emphasized" variant at the same size and metrics,
 * so a swap never reflows the layout; only the weight steps up.
 */
private fun TextStyle.emphasized(weight: FontWeight) = copy(fontWeight = weight)

val AppTypography = base.copy(
    displayLargeEmphasized = base.displayLarge.emphasized(FontWeight.Black),
    displayMediumEmphasized = base.displayMedium.emphasized(FontWeight.Black),
    displaySmallEmphasized = base.displaySmall.emphasized(FontWeight.Black),
    headlineLargeEmphasized = base.headlineLarge.emphasized(FontWeight.Black),
    headlineMediumEmphasized = base.headlineMedium.emphasized(FontWeight.Black),
    headlineSmallEmphasized = base.headlineSmall.emphasized(FontWeight.ExtraBold),
    titleLargeEmphasized = base.titleLarge.emphasized(FontWeight.ExtraBold),
    titleMediumEmphasized = base.titleMedium.emphasized(FontWeight.Bold),
    titleSmallEmphasized = base.titleSmall.emphasized(FontWeight.Bold),
    bodyLargeEmphasized = base.bodyLarge.emphasized(FontWeight.SemiBold),
    bodyMediumEmphasized = base.bodyMedium.emphasized(FontWeight.SemiBold),
    bodySmallEmphasized = base.bodySmall.emphasized(FontWeight.SemiBold),
    labelLargeEmphasized = base.labelLarge.emphasized(FontWeight.Bold),
    labelMediumEmphasized = base.labelMedium.emphasized(FontWeight.Bold),
    labelSmallEmphasized = base.labelSmall.emphasized(FontWeight.Bold),
)
