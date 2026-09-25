/** The rules for dividing an expense into what each person paid and what each person owes. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.SplitMethod
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.utils.apportion
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Decimal places a split value may have. Matches `split_value numeric(14, 4)` in the schema. */
private const val SPLIT_VALUE_DIGITS = 4

private val HUNDRED = BigDecimal(100)

/**
 * Each participant's owed share of [amount] under [method], in exact units of a currency with
 * [minorDigits] decimal places. The keys of [splitValues] are the participants; each value is read
 * as [SplitMethod] describes.
 *
 * Divisions use the largest-remainder method, so the shares add up to [amount] exactly and no two
 * equal shares differ by more than one unit. Leftover units go first to whoever [tieOrder] puts first.
 *
 * Returns null when the values cannot make a split: no participants, a value that is not positive
 * or has more than four decimals, percentages that do not total 100, or exact amounts that do not
 * total [amount] or are finer than the currency allows.
 */
internal fun owedShares(
    amount: BigDecimal,
    method: SplitMethod,
    splitValues: Map<String, BigDecimal>,
    minorDigits: Int,
    tieOrder: Comparator<String>,
): Map<String, BigDecimal>? {
    val isWellFormed = splitValues.isNotEmpty() && amount.fitsDigits(minorDigits) && amount.signum() > 0 &&
        splitValues.values.all { it.signum() > 0 && it.fitsDigits(SPLIT_VALUE_DIGITS) }
    if (!isWellFormed) return null
    return when (method) {
        SplitMethod.EQUAL -> divide(amount, splitValues.mapValues { BigDecimal.ONE }, minorDigits, tieOrder)
        SplitMethod.SHARES -> divide(amount, splitValues, minorDigits, tieOrder)
        SplitMethod.PERCENT ->
            if (splitValues.values.sum().compareTo(HUNDRED) == 0) divide(amount, splitValues, minorDigits, tieOrder) else null
        SplitMethod.EXACT ->
            splitValues.takeIf { values -> values.values.all { it.fitsDigits(minorDigits) } && values.values.sum().compareTo(amount) == 0 }
                ?.mapValues { it.value.setScale(minorDigits) }
    }
}

/**
 * The share rows for an expense of [amount] that [payerId] paid in full, divided by [method] across
 * the participants in [splitValues]. With no [method] the payer bears it alone. The payer need not
 * be a participant, so one housemate can pay for something only the others share.
 *
 * The same rows are previewed and saved, so what the user sees before saving is what is written.
 * Someone who neither paid nor owes anything gets no row. Returns null when [owedShares] does.
 */
internal fun expenseShares(
    amount: BigDecimal,
    payerId: String,
    method: SplitMethod?,
    splitValues: Map<String, BigDecimal>,
    minorDigits: Int,
): List<ExpenseShare>? {
    if (!amount.fitsDigits(minorDigits) || amount.signum() <= 0) return null
    val total = amount.setScale(minorDigits)
    val owed = if (method == null) {
        mapOf(payerId to total)
    } else {
        val payerLast = compareBy<String> { it == payerId }.thenBy { it }
        owedShares(amount, method, splitValues, minorDigits, payerLast) ?: return null
    }
    val zero = BigDecimal.ZERO.setScale(minorDigits)
    return (owed.keys + payerId).distinct()
        .map { userId ->
            ExpenseShare(
                userId = userId,
                paidShare = if (userId == payerId) total else zero,
                owedShare = owed[userId] ?: zero,
                splitValue = if (method == null || method == SplitMethod.EQUAL) null else splitValues[userId],
            )
        }
        .filter { it.paidShare.signum() > 0 || it.owedShare.signum() > 0 }
}

/**
 * The share rows when [payerId] pays [amount] towards [bill]. The bill's split values apply to
 * whatever this payment's amount is, so a 60/40 split of a 100 bill becomes 72/48 when this month's
 * bill is 120. Exact amounts that no longer add up to this payment are scaled the same way.
 */
internal fun recurringPaymentShares(
    bill: RecurringExpense,
    amount: BigDecimal,
    payerId: String,
    minorDigits: Int,
): List<ExpenseShare>? {
    val method = bill.splitMethod?.takeIf { bill.shares.isNotEmpty() }
    val effectiveMethod = if (method == SplitMethod.EXACT && amount.compareTo(bill.amount) != 0) SplitMethod.SHARES else method
    return expenseShares(amount, payerId, effectiveMethod, bill.shares.associate { it.userId to it.splitValue }, minorDigits)
}

/** The `p_shares` payload the expense RPCs take, with amounts as plain decimal strings so no precision is lost. */
internal fun sharesJson(shares: List<ExpenseShare>): JsonArray = buildJsonArray {
    shares.forEach { share ->
        add(buildJsonObject {
            put("user_id", share.userId)
            put("paid_share", share.paidShare.toPlainString())
            put("owed_share", share.owedShare.toPlainString())
            put("split_value", share.splitValue?.toPlainString())
        })
    }
}

private fun divide(
    amount: BigDecimal,
    weights: Map<String, BigDecimal>,
    minorDigits: Int,
    tieOrder: Comparator<String>,
): Map<String, BigDecimal> {
    val units = amount.movePointRight(minorDigits).toBigIntegerExact()
    val integerWeights = weights.mapValues { it.value.movePointRight(SPLIT_VALUE_DIGITS).toBigIntegerExact() }
    return apportion(units, integerWeights, tieOrder).mapValues { it.value.fromUnits(minorDigits) }
}

private fun BigInteger.fromUnits(minorDigits: Int): BigDecimal = toBigDecimal().movePointLeft(minorDigits).setScale(minorDigits)

private fun BigDecimal.fitsDigits(digits: Int): Boolean = stripTrailingZeros().scale() <= digits

private fun Collection<BigDecimal>.sum(): BigDecimal = fold(BigDecimal.ZERO, BigDecimal::add)
