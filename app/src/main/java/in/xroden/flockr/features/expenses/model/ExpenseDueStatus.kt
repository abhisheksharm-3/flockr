/** Where a recurring bill stands against today in the house's time zone. */
package `in`.xroden.flockr.features.expenses.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** [UPCOMING] means due within the bill's reminder window; [SCHEDULED] means due later than that. */
@Serializable
enum class ExpenseDueStatus {
    @SerialName("overdue")
    OVERDUE,

    @SerialName("due_today")
    DUE_TODAY,

    @SerialName("upcoming")
    UPCOMING,

    @SerialName("scheduled")
    SCHEDULED
}
