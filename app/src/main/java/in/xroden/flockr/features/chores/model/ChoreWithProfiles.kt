package `in`.xroden.flockr.features.chores.model

import `in`.xroden.flockr.data.enums.ChoreRecurrence
import `in`.xroden.flockr.data.serialization.InstantSerializer
import `in`.xroden.flockr.data.serialization.LocalDateSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * DTO for chore with nested profile data from database query.
 * Automatically deserialized by Supabase SDK.
 */
@Serializable
internal data class ChoreWithProfiles(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("task_name")
    val taskName: String,
    val description: String? = null,
    @SerialName("assigned_to")
    val assignedTo: String? = null,
    @SerialName("assigned_to_profile")
    val assignedToProfile: ProfileName? = null,
    @SerialName("due_date")
    @Serializable(with = LocalDateSerializer::class)
    val dueDate: LocalDate? = null,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("completed_at")
    @Serializable(with = InstantSerializer::class)
    val completedAt: Instant? = null,
    @SerialName("completed_by")
    val completedBy: String? = null,
    @SerialName("completed_by_profile")
    val completedByProfile: ProfileName? = null,
    @SerialName("recurrence_pattern")
    val recurrencePattern: ChoreRecurrence? = null,
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("created_by_profile")
    val createdByProfile: ProfileName? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
) {
    fun toChore() = Chore(
        id = id,
        houseId = houseId,
        taskName = taskName,
        description = description,
        assignedTo = assignedTo,
        assignedToName = assignedToProfile?.fullName,
        dueDate = dueDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        completedBy = completedBy,
        completedByName = completedByProfile?.fullName,
        recurrencePattern = recurrencePattern,
        createdBy = createdBy,
        createdByName = createdByProfile?.fullName,
        createdAt = createdAt
    )
}

@Serializable
internal data class ProfileName(
    @SerialName("full_name")
    val fullName: String
)
