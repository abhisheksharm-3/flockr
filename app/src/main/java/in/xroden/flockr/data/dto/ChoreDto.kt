package `in`.xroden.flockr.data.dto

import `in`.xroden.flockr.data.enums.ChoreRecurrence
import `in`.xroden.flockr.data.serialization.LocalDateSerializer
import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class ChoreInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("task_name")
    val taskName: String,
    val description: String? = null,
    @SerialName("assigned_to")
    val assignedTo: String? = null,
    @SerialName("due_date")
    @Serializable(with = LocalDateSerializer::class)
    val dueDate: LocalDate? = null,
    @SerialName("recurrence_pattern")
    val recurrencePattern: ChoreRecurrence? = null,
    @SerialName("created_by")
    val createdBy: String
)

@Serializable
data class ChoreUpdate @OptIn(ExperimentalTime::class) constructor(
    @SerialName("task_name")
    val taskName: String? = null,
    val description: String? = null,
    @SerialName("assigned_to")
    val assignedTo: String? = null,
    @SerialName("due_date")
    @Serializable(with = LocalDateSerializer::class)
    val dueDate: LocalDate? = null,
    @SerialName("is_completed")
    val isCompleted: Boolean? = null,
    @SerialName("completed_by")
    val completedBy: String? = null,
    @SerialName("completed_at")
    @Serializable(with = InstantSerializer::class)
    val completedAt: Instant? = null,
    @SerialName("recurrence_pattern")
    val recurrencePattern: ChoreRecurrence? = null
)


