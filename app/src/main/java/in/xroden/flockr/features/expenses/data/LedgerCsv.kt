/** The house ledger as a CSV spreadsheet, one row per expense or payment, for keeping or moving elsewhere. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.ExpenseKind
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import java.math.BigDecimal

/**
 * Oldest first, with a column per person holding what that row did to their balance: paid minus
 * owed, so each column sums to where they stand. Amounts are plain decimals in [currencyCode], with
 * no symbols or grouping, so any spreadsheet reads them as numbers. [members] includes past members.
 */
fun ledgerCsv(expenses: List<Expense>, members: List<MemberWithProfile>, currencyCode: String): String {
    val header = listOf("Date", "Type", "Description", "Category", "Amount", "Currency", "Notes") + members.map { it.displayName }
    val names = members.associate { it.userId to it.displayName }
    val rows = expenses.sortedWith(compareBy({ it.date }, { it.createdAt })).map { expense ->
        val isPayment = expense.kind == ExpenseKind.SETTLEMENT
        val description = if (isPayment) paymentLine(expense, names) else expense.name
        listOf(
            expense.date.toString(),
            if (isPayment) "Payment" else "Expense",
            description,
            expense.category.orEmpty(),
            expense.amount.toPlainString(),
            currencyCode,
            expense.notes.orEmpty(),
        ) + members.map { member -> expense.shareOf(member.userId)?.net?.toPlainString() ?: "0" }
    }
    return (listOf(header) + rows).joinToString("\r\n", postfix = "\r\n") { row -> row.joinToString(",") { it.asCsvField() } }
}

private fun paymentLine(expense: Expense, names: Map<String, String>): String {
    val from = expense.shares.firstOrNull { it.paidShare > BigDecimal.ZERO }?.userId
    val to = expense.shares.firstOrNull { it.owedShare > BigDecimal.ZERO }?.userId
    return "${names[from] ?: "Someone"} paid ${names[to] ?: "someone"}"
}

/**
 * Quoted when it holds a comma, quote or line break, per RFC 4180. Text a spreadsheet would run as a
 * formula (starting =, +, -, or @) gets a leading apostrophe; plain numbers pass through untouched.
 */
private fun String.asCsvField(): String {
    val safe = if (firstOrNull() in FORMULA_STARTS && toBigDecimalOrNull() == null) "'$this" else this
    return if (safe.any { it in ",\"\r\n" }) "\"${safe.replace("\"", "\"\"")}\"" else safe
}

private val FORMULA_STARTS = setOf('=', '+', '-', '@')
