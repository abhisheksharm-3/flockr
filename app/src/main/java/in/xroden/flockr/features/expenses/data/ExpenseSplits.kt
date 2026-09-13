package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * How an expense divides between its payer and the members it is split with.
 *
 * [payerShare] is never stored. The payer holds no split row, so their share is whatever the
 * expense total leaves after [rows]; it is returned here only so the UI can show the payer what
 * they will actually bear, which is not always the same as everyone else's share.
 */
internal data class EqualShares(
    val rows: Map<String, BigDecimal>,
    val payerShare: BigDecimal,
)

/**
 * Divides [amount] equally between [payerId] and [splitWith].
 *
 * Division happens in whole cents and the leftover cents are handed out one each, in user-id
 * order, so the shares sum to exactly [amount] and no two of them differ by more than a cent.
 * Rounding each share independently does not conserve the total: ₹100 across three people rounds
 * to ₹33.33 each, and the cent it drops has to be absorbed by somebody.
 *
 * [splitWith] may contain [payerId]; the payer is counted once either way. When the amount is not
 * positive, or there is nobody else to split with, there are no rows and the payer bears it all.
 */
internal fun equalShares(
    amount: BigDecimal,
    payerId: String,
    splitWith: Collection<String>,
): EqualShares {
    val others = (splitWith - payerId).distinct().sorted()
    val total = amount.setScale(2, RoundingMode.HALF_UP)
    if (others.isEmpty() || total <= BigDecimal.ZERO) {
        return EqualShares(emptyMap(), total.coerceAtLeast(BigDecimal.ZERO.setScale(2)))
    }

    val participants = (others.size + 1).toBigInteger()
    val totalCents = total.movePointRight(2).toBigIntegerExact()
    val baseCents = totalCents / participants
    val leftoverCents = (totalCents % participants).toInt()

    val rows = others.mapIndexed { index, userId ->
        val cents = if (index < leftoverCents) baseCents + BigInteger.ONE else baseCents
        userId to cents.toBigDecimal().movePointLeft(2).setScale(2)
    }.toMap()

    return EqualShares(rows, baseCents.toBigDecimal().movePointLeft(2).setScale(2))
}

/**
 * Builds the `expense_splits` JSON payload sent to the expense RPCs. The payer holds no split row
 * and amounts are written as plain strings so no precision is lost. Shared by the one-time and
 * recurring expense paths so the split semantics cannot diverge between them.
 */
internal fun buildExpenseSplitsJson(
    amount: BigDecimal,
    payerId: String,
    splitWith: List<String>?,
    splitType: ExpenseSplitType?,
    splitAmounts: Map<String, BigDecimal>?
): JsonArray = buildJsonArray {
    if (splitWith.isNullOrEmpty()) return@buildJsonArray

    val rows = when (splitType) {
        ExpenseSplitType.EQUAL -> equalShares(amount, payerId, splitWith).rows
        ExpenseSplitType.AMOUNT -> splitAmounts.orEmpty().filterKeys { it != payerId }
        else -> emptyMap()
    }

    rows.forEach { (userId, owed) ->
        add(buildJsonObject {
            put("user_id", userId)
            put("amount", owed.toPlainString())
        })
    }
}
