/** One expense or payment in a list, worded from the viewer's side as Splitwise does. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.ExpenseKind
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.balanceColor
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.shortMonthLabel

/**
 * The shared expense row: the category in a circle, what it was with the day and who paid under it,
 * and at the end what it did to the viewer's balance, in words as well as colour.
 */
@Composable
fun ExpenseRow(
    expense: Expense,
    members: Map<String, MemberWithProfile>,
    viewerId: String,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payer = members.nameOf(expense.payerId, viewerId)
    val isSettlement = expense.kind == ExpenseKind.SETTLEMENT
    val receiver = expense.shares.firstOrNull { it.owedShare.signum() > 0 }?.userId
    val day = "${expense.date.day} ${expense.date.shortMonthLabel()}"
    ListRow(
        headline = if (isSettlement) "$payer paid ${members.nameInSentence(receiver, viewerId)}" else expense.name,
        supporting = if (isSettlement) "$day · Payment" else "$day · $payer paid ${expense.amount.formatMoney(currencyCode)}",
        leading = { IconBadge(categoryIcon(expense.category), if (isSettlement) BadgeTone.JADE else BadgeTone.SLATE) },
        trailing = { ViewerImpact(expense, viewerId, currencyCode) },
        onClick = onClick,
        modifier = modifier,
    )
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
    if (amount == null) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    } else {
        TrailingAmount(amount.formatMoney(currencyCode), label, color)
    }
}
