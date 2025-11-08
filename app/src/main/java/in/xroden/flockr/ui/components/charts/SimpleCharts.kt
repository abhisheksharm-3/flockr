package `in`.xroden.flockr.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple Pie Chart using Canvas
 */
@Composable
fun SimplePieChart(
    data: Map<String, Double>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "$"
) {
    val total = data.values.sum()
    if (total == 0.0) return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pie Chart
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f
            data.values.forEachIndexed { index, value ->
                val sweepAngle = (value / total * 360).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }
        }

        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = RoundedCornerShape(2.dp),
                            color = colors[index % colors.size]
                        ) {}
                        Text(
                            text = entry.key,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currencySymbol${String.format("%.2f", entry.value)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(entry.value / total * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple Bar Chart using Canvas
 */
@Composable
fun SimpleBarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    currencySymbol: String = "$"
) {
    val maxValue = data.values.maxOrNull() ?: 1.0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.entries.forEach { entry ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        text = "$currencySymbol${String.format("%.2f", entry.value)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val barWidth = (entry.value / maxValue * size.width).toFloat()
                    drawRoundRect(
                        color = color.copy(alpha = 0.2f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                }
            }
        }
    }
}

/**
 * Simple Donut Chart using Canvas
 */
@Composable
fun SimpleDonutChart(
    data: Map<String, Double>,
    colors: List<Color>,
    centerText: String = "",
    centerSubtext: String = "",
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0.0) return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Donut Chart
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasSize = size.minDimension
                val outerRadius = canvasSize / 2
                val innerRadius = outerRadius * 0.6f
                val center = Offset(size.width / 2, size.height / 2)

                var startAngle = -90f
                data.values.forEachIndexed { index, value ->
                    val sweepAngle = (value / total * 360).toFloat()

                    // Draw outer arc
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2)
                    )

                    startAngle += sweepAngle
                }

                // Draw inner circle (creates donut hole)
                drawCircle(
                    color = Color.White,
                    radius = innerRadius,
                    center = center
                )
            }

            // Center text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (centerText.isNotEmpty()) {
                    Text(
                        text = centerText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (centerSubtext.isNotEmpty()) {
                    Text(
                        text = centerSubtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Legend
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = RoundedCornerShape(2.dp),
                            color = colors[index % colors.size]
                        ) {}
                        Text(
                            text = entry.key,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "$${String.format("%.2f", entry.value)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

