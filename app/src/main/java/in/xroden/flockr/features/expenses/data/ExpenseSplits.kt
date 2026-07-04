package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Builds the `expense_splits` JSON payload sent to the expense RPCs. The payer holds no
 * split row (they keep their own share implicitly), and amounts are written as plain
 * strings so no precision is lost. Shared by one-time and recurring expense paths so the
 * split semantics can never diverge between them.
 */
internal fun buildExpenseSplitsJson(
    amount: BigDecimal,
    payerId: String,
    splitWith: List<String>?,
    splitType: ExpenseSplitType?,
    splitAmounts: Map<String, BigDecimal>?
): JsonArray = buildJsonArray {
    if (!splitWith.isNullOrEmpty()) {
        when (splitType) {
            ExpenseSplitType.EQUAL -> {
                val uniqueParticipants = (splitWith + payerId).distinct()
                val splitAmount = amount.divide(BigDecimal(uniqueParticipants.size), 2, RoundingMode.HALF_UP)
                uniqueParticipants.filter { it != payerId }.forEach { participantId ->
                    add(buildJsonObject {
                        put("user_id", participantId)
                        put("amount", splitAmount.toPlainString())
                    })
                }
            }
            ExpenseSplitType.AMOUNT -> {
                splitAmounts?.forEach { (splitUserId, splitAmount) ->
                    if (splitUserId != payerId) {
                        add(buildJsonObject {
                            put("user_id", splitUserId)
                            put("amount", splitAmount.toPlainString())
                        })
                    }
                }
            }
            else -> { }
        }
    }
}
