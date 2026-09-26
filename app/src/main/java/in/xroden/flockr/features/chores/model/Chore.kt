/** A house chore: who does it, when, how often, and whose turn is next. */
package `in`.xroden.flockr.features.chores.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.features.chores.model.ChoreRecurrence
import `in`.xroden.flockr.core.serialization.InstantSerializer
import `in`.xroden.flockr.core.serialization.LocalDateSerializer
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [rotation] is the order members take turns in when [recurrencePattern] repeats the chore;
 * completing it schedules the next occurrence for the next current member in it. [effortPoints],
 * from 1 to 10, is how much the chore counts on the leaderboard.
 */
@Immutable
@Serializable
data class Chore(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("task_name")
    val taskName: String,
    val description: String? = null,
    @SerialName("due_date")
    @Serializable(with = LocalDateSerializer::class)
    val dueDate: LocalDate? = null,
    @SerialName("recurrence_pattern")
    val recurrencePattern: ChoreRecurrence? = null,
    val rotation: List<String> = emptyList(),
    @SerialName("effort_points")
    val effortPoints: Int = 1,
    @SerialName("assigned_to")
    val assignedTo: String? = null,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("completed_at")
    @Serializable(with = InstantSerializer::class)
    val completedAt: Instant? = null,
    @SerialName("completed_by")
    val completedBy: String? = null,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)
