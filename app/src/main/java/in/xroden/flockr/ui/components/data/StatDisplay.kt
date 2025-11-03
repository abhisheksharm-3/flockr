package `in`.xroden.flockr.ui.components.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.xroden.flockr.ui.theme.NumericDisplayMedium
import `in`.xroden.flockr.ui.theme.NumericDisplaySmall
import `in`.xroden.flockr.ui.theme.OverlineLabel

/**
 * StatDisplay - Component for displaying key metrics/statistics
 * Clean, data-rich design inspired by fold.money
 */
@Composable
fun StatDisplay(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    size: StatSize = StatSize.Medium
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label/Category
        Text(
            text = label.uppercase(),
            style = OverlineLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        
        // Main Value
        Text(
            text = value,
            style = when (size) {
                StatSize.Large -> NumericDisplayMedium
                StatSize.Medium -> NumericDisplaySmall
                StatSize.Small -> MaterialTheme.typography.headlineSmall
            },
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        
        // Subtitle/Context
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact stat for grid layouts
 */
@Composable
fun CompactStatDisplay(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class StatSize {
    Large, Medium, Small
}

