/** The split part of the expense and bill forms: whether it is split, how, between whom, and what was typed. */
package `in`.xroden.flockr.features.expenses.presentation

import `in`.xroden.flockr.data.enums.SplitMethod
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.utils.parseDecimal
import `in`.xroden.flockr.utils.parseMoney
import java.math.BigDecimal

private val HUNDRED = BigDecimal(100)

/**
 * [values] holds the text typed for each participant under [method]: an amount, a percentage or a
 * number of shares. It stays text so a half-typed value survives recomposition.
 */
data class SplitDraft(
    val isEnabled: Boolean = false,
    val method: SplitMethod = SplitMethod.EQUAL,
    val participantIds: Set<String> = emptySet(),
    val values: Map<String, String> = emptyMap(),
) {
    /** The method to save, or null when the cost is not split. */
    val savedMethod: SplitMethod? get() = method.takeIf { isEnabled }

    /** Each participant's value as [method] reads it, or null while one is missing or not a number. */
    fun parsedValues(currencyCode: String): Map<String, BigDecimal>? = when (method) {
        SplitMethod.EQUAL -> participantIds.associateWith { BigDecimal.ONE }
        else -> participantIds.associateWith { userId -> parse(values[userId].orEmpty(), currencyCode) ?: return null }
    }

    /**
     * What remains to assign before exact amounts reach [total] or percentages reach 100, negative
     * when over. Null for the methods that always add up.
     */
    fun unassigned(total: BigDecimal?, currencyCode: String): BigDecimal? {
        if (!isEnabled) return null
        val target = when (method) {
            SplitMethod.EXACT -> total ?: return null
            SplitMethod.PERCENT -> HUNDRED
            SplitMethod.EQUAL, SplitMethod.SHARES -> return null
        }
        return target - participantIds.mapNotNull { parse(values[it].orEmpty(), currencyCode) }.fold(BigDecimal.ZERO, BigDecimal::add)
    }

    /** Switches method, clearing values typed for the old one; a shares split starts from each member's default weight. */
    fun withMethod(newMethod: SplitMethod, members: List<MemberWithProfile>): SplitDraft = copy(
        method = newMethod,
        values = if (newMethod == SplitMethod.SHARES) {
            members.associate { it.userId to it.defaultSplitWeight.stripTrailingZeros().toPlainString() }
        } else {
            emptyMap()
        },
    )

    fun withParticipant(userId: String, isIncluded: Boolean): SplitDraft =
        copy(participantIds = if (isIncluded) participantIds + userId else participantIds - userId)

    fun withValue(userId: String, text: String): SplitDraft = copy(values = values + (userId to text))

    private fun parse(text: String, currencyCode: String): BigDecimal? =
        if (method == SplitMethod.EXACT) parseMoney(text, currencyCode) else parseDecimal(text)

    companion object {
        /** A new split, on and equal across everyone currently in the house, the default Splitwise uses. */
        fun everyone(members: List<MemberWithProfile>) = SplitDraft(isEnabled = true, participantIds = members.filter { it.isActive }.map { it.userId }.toSet())
    }
}
