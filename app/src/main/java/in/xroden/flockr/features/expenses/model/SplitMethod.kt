/** How an expense's cost is divided between the people it is for. */
package `in`.xroden.flockr.features.expenses.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Each method reads a per-person split value differently: [EQUAL] ignores it, [EXACT] takes it as
 * the amount owed, [PERCENT] as a percentage of the total, and [SHARES] as a weight, such as 2 for
 * a couple sharing a room. [label] names it on a choice chip and [phrase] finishes "Split …".
 */
@Serializable
enum class SplitMethod(val label: String, val phrase: String) {
    @SerialName("equal")
    EQUAL("Equal", "equally"),

    @SerialName("exact")
    EXACT("Amounts", "by exact amounts"),

    @SerialName("percent")
    PERCENT("Percent", "by percentage"),

    @SerialName("shares")
    SHARES("Shares", "by shares")
}
