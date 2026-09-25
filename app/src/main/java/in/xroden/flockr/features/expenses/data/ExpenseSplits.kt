/** The rules for dividing an expense between its payer and the members it is split with. */
package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.utils.apportion
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * How an expense divides between its payer and the other members.
 *
 * Only [rows] are stored. The payer holds no split row: their share is whatever the total leaves
 * after [rows], and it is returned so the UI can show the payer what they will actually bear.
 */
internal data class SplitShares(
    val rows: Map<String, BigDecimal>,
    val payerShare: BigDecimal,
)

/**
 * Divides [amount] equally between [payerId] and [splitWith] in the currency's smallest unit.
 *
 * [minorDigits] is the currency's number of minor-unit digits: 2 for cents, 0 for yen. Dividing in
 * cents for a currency without them would store fractions of a yen that no display can show, and
 * the shares would appear not to add up. Leftover units go to the other members before the payer,
 * so the shares sum to exactly [amount] and no two differ by more than one unit.
 *
 * [splitWith] may contain [payerId]; the payer is counted once. With no one else to split with, or
 * an amount that is not positive, there are no rows and the payer bears it all.
 */
internal fun equalShares(
    amount: BigDecimal,
    payerId: String,
    splitWith: Collection<String>,
    minorDigits: Int,
): SplitShares {
    val participants = (splitWith + payerId).distinct()
    if (participants.size < 2 || amount.signum() <= 0) return payerBearsAll(amount, minorDigits)
    val payerLast = compareBy<String> { it == payerId }.thenBy { it }
    return sharesOf(amount, payerId, participants.associateWith { BigInteger.ONE }, minorDigits, payerLast)
}

/**
 * Divides [amount] in proportion to [weights], in the currency's smallest unit.
 *
 * This is how a recurring bill with a custom split is paid: the amounts saved on the template are
 * proportions, so a 60/40 split of a 100 bill becomes 72/48 when this month's bill is 120. The
 * shares always sum to exactly [amount]; ties go to the lower user id so the result is stable.
 *
 * [payerId] need not appear in [weights], in which case the payer bears nothing. Returns null when
 * no weight is positive or any weight is negative.
 */
internal fun proportionalShares(
    amount: BigDecimal,
    payerId: String,
    weights: Map<String, BigDecimal>,
    minorDigits: Int,
): SplitShares? {
    if (weights.values.any { it.signum() < 0 } || weights.values.none { it.signum() > 0 }) return null
    val weightUnits = weights.mapValues { it.value.toMinorUnits(minorDigits) }
    return sharesOf(amount, payerId, weightUnits, minorDigits, naturalOrder())
}

private fun sharesOf(
    amount: BigDecimal,
    payerId: String,
    weights: Map<String, BigInteger>,
    minorDigits: Int,
    tieOrder: Comparator<String>,
): SplitShares {
    val shares = apportion(amount.toMinorUnits(minorDigits), weights, tieOrder).mapValues { it.value.fromMinorUnits(minorDigits) }
    return SplitShares(
        rows = shares.filterKeys { it != payerId },
        payerShare = shares[payerId] ?: BigDecimal.ZERO.setScale(minorDigits),
    )
}

private fun payerBearsAll(amount: BigDecimal, minorDigits: Int) =
    SplitShares(emptyMap(), amount.max(BigDecimal.ZERO).setScale(minorDigits, RoundingMode.HALF_UP))

private fun BigDecimal.toMinorUnits(minorDigits: Int): BigInteger =
    setScale(minorDigits, RoundingMode.HALF_UP).movePointRight(minorDigits).toBigIntegerExact()

private fun BigInteger.fromMinorUnits(minorDigits: Int): BigDecimal =
    toBigDecimal().movePointLeft(minorDigits).setScale(minorDigits)

/**
 * How one payment of the recurring bill [template] divides when [payerId] pays [amount].
 *
 * The bill belongs to its creator and the members it is split with, whoever happens to pay this
 * time. An equal split weights each participant the same; a custom split uses the template's saved
 * amounts as proportions, with the creator holding whatever those amounts leave of the template's
 * total. Returns null when the template's split cannot be applied.
 */
internal fun recurringPaymentShares(
    template: RecurringExpense,
    amount: BigDecimal,
    payerId: String,
    minorDigits: Int,
): SplitShares? {
    val splitWith = template.splitWith.orEmpty()
    if (splitWith.isEmpty() || template.splitType == null) return payerBearsAll(amount, minorDigits)
    val weights: Map<String, BigDecimal> = when (template.splitType) {
        ExpenseSplitType.EQUAL -> (splitWith + template.createdBy).distinct().associateWith { BigDecimal.ONE }
        ExpenseSplitType.AMOUNT, ExpenseSplitType.CUSTOM -> {
            val listed = template.splitAmounts.orEmpty()
            val creatorRemainder = template.amount - listed.values.fold(BigDecimal.ZERO, BigDecimal::add)
            if (creatorRemainder.signum() > 0) {
                listed + (template.createdBy to (listed[template.createdBy].orZero() + creatorRemainder))
            } else listed
        }
        ExpenseSplitType.PERCENTAGE -> return null
    }
    return proportionalShares(amount, payerId, weights, minorDigits)
}

private fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO

/**
 * Validates explicit per-member [amounts] against [amount] and returns the resulting shares.
 *
 * The payer's share is whatever the other members' amounts leave. If the payer is listed with an
 * amount of their own it must equal that remainder, so the listed amounts total [amount] exactly.
 * Returns null when an amount is negative or has more decimals than [minorDigits], when the other
 * members' amounts exceed the expense, or when the payer's own listed amount does not match.
 */
internal fun customShares(
    amount: BigDecimal,
    payerId: String,
    amounts: Map<String, BigDecimal>,
    minorDigits: Int,
): SplitShares? {
    val isWellFormed = amounts.values.all { it.signum() >= 0 && it.stripTrailingZeros().scale() <= minorDigits }
    if (!isWellFormed || amount.signum() <= 0) return null

    val rows = amounts.filterKeys { it != payerId }.mapValues { it.value.setScale(minorDigits) }
    val payerShare = amount.setScale(minorDigits, RoundingMode.HALF_UP) - rows.values.fold(BigDecimal.ZERO, BigDecimal::add)
    if (payerShare.signum() < 0) return null

    val listedPayerShare = amounts[payerId]
    if (listedPayerShare != null && listedPayerShare.compareTo(payerShare) != 0) return null

    return SplitShares(rows, payerShare)
}

/**
 * Plans how [amount] divides for a split of [splitType] across [members], in a currency with
 * [minorDigits] minor-unit digits. The preview and the saved rows both come from here, so what the
 * user sees before saving is exactly what is written.
 *
 * Returns null when the custom amounts break the rules in [customShares], or for a percentage
 * split, which no screen produces and this app does not support.
 */
internal fun planSplit(
    amount: BigDecimal,
    payerId: String,
    splitType: ExpenseSplitType?,
    members: Collection<String>,
    customAmounts: Map<String, BigDecimal>?,
    minorDigits: Int,
): SplitShares? = when {
    splitType == null || members.none { it != payerId } -> payerBearsAll(amount, minorDigits)
    splitType == ExpenseSplitType.EQUAL -> equalShares(amount, payerId, members, minorDigits)
    splitType == ExpenseSplitType.PERCENTAGE -> null
    else -> {
        val amounts = customAmounts.orEmpty().filterKeys { it in members || it == payerId }
        val isEveryMemberPriced = members.all { it == payerId || it in amounts }
        if (isEveryMemberPriced) customShares(amount, payerId, amounts, minorDigits) else null
    }
}

/** The `expense_splits` payload the expense RPCs take, as plain decimal strings so no precision is lost. */
internal fun splitRowsJson(rows: Map<String, BigDecimal>): JsonArray = buildJsonArray {
    rows.forEach { (userId, owed) ->
        add(buildJsonObject {
            put("user_id", userId)
            put("amount", owed.toPlainString())
        })
    }
}
