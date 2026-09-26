/**
 * The monthly report's category chart. AndroidX ships no charting library, so the ring is drawn
 * on a Canvas.
 */
package `in`.xroden.flockr.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.ui.theme.Motion
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.apportion
import `in`.xroden.flockr.utils.formatMoney
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

private const val FullTurn = 360f
private const val RingStartAngle = -90f
private const val SegmentGapDegrees = 6f
private const val MinSegmentDegrees = 2f
private const val RingThicknessFraction = 0.16f
private const val PercentScale = 100

/** One segment of the ring. [key] identifies it for [onItemClick]; [label] is what the chart shows. */
data class ChartEntry(val key: String, val label: String, val value: BigDecimal)

private val RingSize = 200.dp
private val LegendSwatchSize = 12.dp

/**
 * Spend split by category, as a segmented ring with the period total in its centre.
 *
 * Segments follow the order of [data], which also decides the palette colour each one lands on.
 * [onItemClick] receives the [ChartEntry.key] of the legend row tapped.
 */
@Composable
fun SimplePieChart(
    data: List<ChartEntry>,
    modifier: Modifier = Modifier,
    currencyCode: String,
    onItemClick: ((String) -> Unit)? = null
) {
    val total = data.sumOf { it.value }
    if (total.signum() <= 0) return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Box(
            modifier = Modifier.size(RingSize),
            contentAlignment = Alignment.Center
        ) {
            SegmentRing(data = data)
            RingTotal(total = total, currencyCode = currencyCode)
        }
        CategoryLegend(
            data = data,
            currencyCode = currencyCode,
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun SegmentRing(data: List<ChartEntry>) {
    val palette = chartPalette()
    val growth = rememberGrowth(data)
    val total = data.sumOf { it.value }.toFloat()

    Canvas(modifier = Modifier.size(RingSize)) {
        val thickness = size.minDimension * RingThicknessFraction
        val arcTopLeft = Offset(thickness / 2f, thickness / 2f)
        val arcSize = Size(size.width - thickness, size.height - thickness)
        val stroke = Stroke(width = thickness, cap = StrokeCap.Round)

        var startAngle = RingStartAngle
        data.forEachIndexed { index, entry ->
            val value = entry.value
            val sweep = value.toFloat() / total * FullTurn * growth
            drawArc(
                color = palette[index % palette.size],
                startAngle = startAngle + SegmentGapDegrees / 2f,
                sweepAngle = (sweep - SegmentGapDegrees).coerceAtLeast(MinSegmentDegrees),
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun RingTotal(total: BigDecimal, currencyCode: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = total.formatMoney(currencyCode),
            style = MaterialTheme.typography.titleLargeEmphasized
        )
        Text(
            text = "Total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryLegend(
    data: List<ChartEntry>,
    currencyCode: String,
    onItemClick: ((String) -> Unit)?
) {
    val palette = chartPalette()
    val percents = remember(data) { wholePercents(data) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        data.forEachIndexed { index, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = onItemClick != null) { onItemClick?.invoke(entry.key) }
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(LegendSwatchSize)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(palette[index % palette.size])
                    )
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.value.formatMoney(currencyCode),
                        style = MaterialTheme.typography.bodyMediumEmphasized
                    )
                    Text(
                        text = "${percents[entry.key]}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Category colours in the order segments are drawn, wrapping by modulo so any number of categories
 * renders. Every entry is a theme role, so the charts restyle with the colour scheme.
 */
@Composable
@ReadOnlyComposable
private fun chartPalette(): List<Color> = with(MaterialTheme.colorScheme) {
    listOf(primary, MaterialTheme.flockrColors.sun, tertiary, error, secondary, primaryContainer, tertiaryContainer)
}

/**
 * A 0..1 factor that runs on the theme's spatial spec when the chart first appears and again
 * whenever [key] changes, so switching month re-draws the chart rather than snapping it.
 */
@Composable
private fun rememberGrowth(key: Any?): Float {
    val growth = remember { Animatable(0f) }
    val spec = Motion.spatial

    LaunchedEffect(key) {
        growth.snapTo(0f)
        growth.animateTo(targetValue = 1f, animationSpec = spec)
    }
    return growth.value.coerceIn(0f, 1f)
}

/**
 * Each category's share of the total as whole percentages that add up to exactly 100. Truncating
 * each share separately does not: three equal categories come out as 33 + 33 + 33 = 99.
 */
private fun wholePercents(data: List<ChartEntry>): Map<String, BigInteger> =
    apportion(
        total = PercentScale.toBigInteger(),
        weights = data.associate { it.key to it.value.setScale(2, RoundingMode.HALF_UP).unscaledValue().max(BigInteger.ZERO) },
        tieOrder = naturalOrder()
    )
