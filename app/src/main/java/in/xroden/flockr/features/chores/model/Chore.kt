package `in`.xroden.flockr.features.chores.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Chore(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("task_name")
    val taskName: String,
    val description: String? = null,
    @SerialName("assigned_to")
    val assignedTo: String? = null,
    @SerialName("assigned_to_name")
    val assignedToName: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("completed_by")
    val completedBy: String? = null,
    @SerialName("completed_by_name")
    val completedByName: String? = null,
    @SerialName("recurrence_pattern")
    val recurrencePattern: String? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_by_name")
    val createdByName: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

