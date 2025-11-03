package `in`.xroden.flockr.ui.components.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.ui.theme.*

/**
 * BalanceDisplay - Component for showing financial balances with +/- indicators
 * Positive = green (money owed to you), Negative = red (money you owe)
 */
@Composable
fun BalanceDisplay(
    amount: Double,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier,
    label: String? = null,
    showIcon: Boolean = true,
    size: BalanceSize = BalanceSize.Medium
) {
    val isPositive = amount > 0
    val isNeutral = amount == 0.0
    
    val backgroundColor = when {
        isNeutral -> MaterialTheme.colorScheme.surfaceVariant
        isPositive -> PositiveGreenLight
        else -> NegativeRedLight
    }
    
    val textColor = when {
        isNeutral -> NeutralBalance
        isPositive -> PositiveGreen
        else -> NegativeRed
    }
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        label?.let {
            Text(
                text = it.uppercase(),
                style = OverlineLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showIcon && !isNeutral) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (isPositive) "Positive" else "Negative",
                    tint = textColor,
                    modifier = Modifier.size(
                        when (size) {
                            BalanceSize.Large -> 32.dp
                            BalanceSize.Medium -> 24.dp
                            BalanceSize.Small -> 18.dp
                        }
                    )
                )
            }
            
            Text(
                text = formatBalance(amount, currencySymbol),
                style = when (size) {
                    BalanceSize.Large -> NumericDisplayMedium
                    BalanceSize.Medium -> NumericDisplaySmall
                    BalanceSize.Small -> NumericBody
                },
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Compact balance for inline display
 */
@Composable
fun CompactBalanceDisplay(
    amount: Double,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val isPositive = amount > 0
    val isNeutral = amount == 0.0
    
    val textColor = when {
        isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
        isPositive -> PositiveGreen
        else -> NegativeRed
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!isNeutral) {
            Icon(
                imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = formatBalance(amount, currencySymbol),
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class BalanceSize {
    Large, Medium, Small
}

private fun formatBalance(amount: Double, currencySymbol: String): String {
    val absAmount = kotlin.math.abs(amount)
    val sign = if (amount < 0) "-" else if (amount > 0) "+" else ""
    return "$sign$currencySymbol%.2f".format(absAmount)
}

