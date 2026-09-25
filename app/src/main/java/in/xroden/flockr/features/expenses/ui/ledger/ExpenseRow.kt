/** One expense or payment in a list, worded from the viewer's side as Splitwise does. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.ExpenseKind
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.shortMonthLabel
import java.math.BigDecimal

/** "You" for the viewer, the member's name otherwise, and "A former housemate" for someone no longer on the roster. */
fun Map<String, MemberWithProfile>.nameOf(userId: String?, viewerId: String): String = when (userId) {
    viewerId -> "You"
    null -> "Someone"
    else -> get(userId)?.displayName ?: "A former housemate"
}

/** The colour for a balance: the primary colour when others owe, the error colour when you owe. */
@Composable
fun balanceColor(net: BigDecimal): Color = when (net.signum()) {
    1 -> MaterialTheme.colorScheme.primary
    -1 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun ExpenseRow(
    expense: Expense,
    members: Map<String, MemberWithProfile>,
    viewerId: String,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        DateBlock(expense)
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
            Box(Modifier.size(ComponentHeight.avatar), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(expense.category), contentDescription = null, modifier = Modifier.size(IconSize.md))
            }
        }
        Column(Modifier.weight(1f)) {
            val payer = members.nameOf(expense.payerId, viewerId)
            val isSettlement = expense.kind == ExpenseKind.SETTLEMENT
            val receiver = expense.shares.firstOrNull { it.owedShare.signum() > 0 }?.userId
            Text(
                text = if (isSettlement) "$payer paid ${if (receiver == viewerId) "you" else members.nameOf(receiver, viewerId)}" else expense.name,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (isSettlement) "Payment" else "$payer paid ${expense.amount.formatMoney(currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ViewerImpact(expense, viewerId, currencyCode)
    }
}

@Composable
private fun DateBlock(expense: Expense) {
    Column(Modifier.width(Spacing.xxxl), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(expense.date.shortMonthLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(expense.date.day.toString(), style = MaterialTheme.typography.titleMediumEmphasized)
    }
}

/** What the expense did to the viewer's balance: lent, borrowed, or nothing at all. */
@Composable
private fun ViewerImpact(expense: Expense, viewerId: String, currencyCode: String) {
    val share = expense.shareOf(viewerId)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val (label, amount, color) = when {
        share == null -> Triple("not involved", null, muted)
        expense.kind == ExpenseKind.SETTLEMENT && share.paidShare.signum() > 0 -> Triple("you paid", share.paidShare, muted)
        expense.kind == ExpenseKind.SETTLEMENT -> Triple("you received", share.owedShare, muted)
        share.net.signum() > 0 -> Triple("you lent", share.net, balanceColor(share.net))
        share.net.signum() < 0 -> Triple("you borrowed", share.net.negate(), balanceColor(share.net))
        else -> Triple("no balance", null, muted)
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.End)
        amount?.let { Text(it.formatMoney(currencyCode), style = MaterialTheme.typography.bodyMediumEmphasized, color = color) }
    }
}
