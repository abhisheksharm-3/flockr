package `in`.xroden.flockr.data.model

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
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("created_at")
    val createdAt: String
)

