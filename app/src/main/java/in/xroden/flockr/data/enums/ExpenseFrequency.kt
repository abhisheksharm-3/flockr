/** How often a recurring bill falls due. */
package `in`.xroden.flockr.data.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Monthly and longer cadences keep the first due date's day of month, clamped to shorter months. */
@Serializable
enum class ExpenseFrequency(val label: String) {
    @SerialName("daily")
    DAILY("Daily"),

    @SerialName("weekly")
    WEEKLY("Weekly"),

    @SerialName("biweekly")
    BIWEEKLY("Every two weeks"),

    @SerialName("monthly")
    MONTHLY("Monthly"),

    @SerialName("quarterly")
    QUARTERLY("Quarterly"),

    @SerialName("semiannual")
    SEMIANNUAL("Every six months"),

    @SerialName("yearly")
    YEARLY("Yearly"),

    @SerialName("custom")
    CUSTOM("Custom")
}
