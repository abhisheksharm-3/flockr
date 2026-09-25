/** How an expense's cost is divided between the people it is for. */
package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Each method reads a per-person split value differently: [EQUAL] ignores it, [EXACT] takes it as
 * the amount owed, [PERCENT] as a percentage of the total, and [SHARES] as a weight, such as 2 for
 * a couple sharing a room.
 */
@Serializable
enum class SplitMethod(val label: String) {
    @SerialName("equal")
    EQUAL("Equally"),

    @SerialName("exact")
    EXACT("Exact amounts"),

    @SerialName("percent")
    PERCENT("Percentages"),

    @SerialName("shares")
    SHARES("Shares")
}
