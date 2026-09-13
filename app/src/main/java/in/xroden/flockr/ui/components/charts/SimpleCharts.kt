/**
 * Expense charts for the monthly report. AndroidX ships no charting library, so the category ring
 * is drawn on a Canvas; the per-member bars are Material's own linear progress indicator.
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
import `in`.xroden.flockr.utils.formatAmount
import java.math.BigDecimal

private const val FullTurn = 360f
private const val RingStartAngle = -90f
private const val SegmentGapDegrees = 6f
private const val MinSegmentDegrees = 2f
private const val RingThicknessFraction = 0.16f
private const val PercentScale = 100

private val RingSize = 200.dp
private val BarHeight = 12.dp
private val LegendSwatchSize = 12.dp

/**
 * Spend split by category, as a segmented ring with the period total in its centre.
 *
 * Segments follow the iteration order of [data], so pass an ordered map to control the palette
 * each category lands on. [onItemClick] receives the category key of the legend row tapped.
 */
@Composable
fun SimplePieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    onItemClick: ((String) -> Unit)? = null
) {
    val total = data.values.sum()
    if (total <= 0.0) return

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
            RingTotal(total = total, currencySymbol = currencySymbol)
        }
        CategoryLegend(
            data = data,
            currencySymbol = currencySymbol,
            onItemClick = onItemClick
        )
    }
}

/**
 * Spend per member, each bar scaled against the largest value in [data].
 *
 * [onItemClick] receives the member key of the row tapped.
 */
@Composable
fun SimpleBarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$",
    onItemClick: ((String) -> Unit)? = null
) {
    val maxValue = data.values.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val growth = rememberGrowth(data)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        data.entries.forEach { entry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(enabled = onItemClick != null) { onItemClick?.invoke(entry.key) }
                    .padding(vertical = Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.key,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatAmount(BigDecimal.valueOf(entry.value), currencySymbol),
                        style = MaterialTheme.typography.bodyMediumEmphasized
                    )
                }
                LinearProgressIndicator(
                    progress = { (entry.value / maxValue).toFloat() * growth },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BarHeight),
                    drawStopIndicator = {}
                )
            }
        }
    }
}

@Composable
private fun SegmentRing(data: Map<String, Double>) {
    val palette = chartPalette()
    val growth = rememberGrowth(data)
    val total = data.values.sum()

    Canvas(modifier = Modifier.size(RingSize)) {
        val thickness = size.minDimension * RingThicknessFraction
        val arcTopLeft = Offset(thickness / 2f, thickness / 2f)
        val arcSize = Size(size.width - thickness, size.height - thickness)
        val stroke = Stroke(width = thickness, cap = StrokeCap.Round)

        var startAngle = RingStartAngle
        data.values.forEachIndexed { index, value ->
            val sweep = (value / total * FullTurn).toFloat() * growth
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
private fun RingTotal(total: Double, currencySymbol: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatAmount(BigDecimal.valueOf(total), currencySymbol),
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
    data: Map<String, Double>,
    currencySymbol: String,
    onItemClick: ((String) -> Unit)?
) {
    val palette = chartPalette()
    val total = data.values.sum()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        data.entries.forEachIndexed { index, entry ->
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
                        text = entry.key,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatAmount(BigDecimal.valueOf(entry.value), currencySymbol),
                        style = MaterialTheme.typography.bodyMediumEmphasized
                    )
                    Text(
                        text = "${(entry.value / total * PercentScale).toInt()}%",
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
    listOf(primary, tertiary, secondary, primaryContainer, tertiaryContainer, secondaryContainer)
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
