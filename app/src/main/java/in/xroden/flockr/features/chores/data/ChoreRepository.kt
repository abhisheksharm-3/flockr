package `in`.xroden.flockr.features.chores.data

import `in`.xroden.flockr.data.dto.ChoreInsert
import `in`.xroden.flockr.data.dto.ChoreUpdate
import `in`.xroden.flockr.features.chores.model.Chore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.launch

@Singleton
class ChoreRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    @Serializable
    private data class ChoreWithProfiles(
        val id: String,
        @SerialName("house_id")
        val houseId: String,
        @SerialName("task_name")
        val taskName: String,
        val description: String? = null,
        @SerialName("assigned_to")
        val assignedTo: String? = null,
        @SerialName("due_date")
        val dueDate: LocalDate? = null,
        @SerialName("is_completed")
        val isCompleted: Boolean = false,
        @SerialName("completed_at")
        val completedAt: Instant? = null,
        @SerialName("completed_by")
        val completedBy: String? = null,
        @SerialName("recurrence_pattern")
        val recurrencePattern: String? = null,
        @SerialName("created_by")
        val createdBy: String? = null,
        @SerialName("created_at")
        val createdAt: Instant,
        @SerialName("assigned_to_name")
        val assignedToName: String? = null,
        @SerialName("completed_by_name")
        val completedByName: String? = null,
        @SerialName("created_by_name")
        val createdByName: String? = null
    )

    fun getChoresFlow(houseId: String): Flow<Result<List<Chore>>> = callbackFlow {
        val channelId = "chores_$houseId"
        val channel = supabase.realtime.channel(channelId)

        try {
            // Emit initial data
            send(getChores(houseId))

            // Setup realtime listener with database-level filter
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chores"
                filter = "house_id=eq.$houseId"
            }

            channel.subscribe(blockUntilSubscribed = true)

            changeFlow.collect {
                kotlinx.coroutines.delay(100)
                send(getChores(houseId))
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    suspend fun getChores(houseId: String): Result<List<Chore>> {
        return try {
            val response = supabase.from("chores")
                .select(Columns.raw("""
                    *,
                    assigned_to_profile:profiles!chores_assigned_to_fkey(full_name),
                    completed_by_profile:profiles!chores_completed_by_fkey(full_name),
                    created_by_profile:profiles!chores_created_by_fkey(full_name)
                """.trimIndent())) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("due_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()

            val chores = response.map { obj ->
                val assignedToName = obj["assigned_to_profile"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.jsonObject?.get("full_name")?.jsonPrimitive?.content
                val completedByName = obj["completed_by_profile"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.jsonObject?.get("full_name")?.jsonPrimitive?.content
                val createdByName = obj["created_by_profile"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.jsonObject?.get("full_name")?.jsonPrimitive?.content

                Chore(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    houseId = obj["house_id"]?.jsonPrimitive?.content ?: "",
                    taskName = obj["task_name"]?.jsonPrimitive?.content ?: "",
                    description = obj["description"]?.jsonPrimitive?.contentOrNull,
                    assignedTo = obj["assigned_to"]?.jsonPrimitive?.contentOrNull,
                    assignedToName = assignedToName,
                    dueDate = obj["due_date"]?.jsonPrimitive?.contentOrNull?.let { LocalDate.parse(it) },
                    isCompleted = obj["is_completed"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                    completedAt = obj["completed_at"]?.jsonPrimitive?.contentOrNull?.let { Instant.parse(it) },
                    completedBy = obj["completed_by"]?.jsonPrimitive?.contentOrNull,
                    completedByName = completedByName,
                    recurrencePattern = obj["recurrence_pattern"]?.jsonPrimitive?.contentOrNull?.let {
                        try {
                            `in`.xroden.flockr.data.enums.ChoreRecurrence.valueOf(it.uppercase())
                        } catch (e: Exception) {
                            null
                        }
                    },
                    createdBy = obj["created_by"]?.jsonPrimitive?.contentOrNull,
                    createdByName = createdByName,
                    createdAt = obj["created_at"]?.jsonPrimitive?.content?.let { Instant.parse(it) }
                        ?: Instant.DISTANT_PAST
                )
            }

            Result.success(chores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChore(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: LocalDate?,
        recurrencePattern: `in`.xroden.flockr.data.enums.ChoreRecurrence?,
        assignedTo: String?
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("User not authenticated"))

            supabase.from("chores")
                .insert(
                    ChoreInsert(
                        houseId = houseId,
                        taskName = taskName,
                        description = description,
                        dueDate = dueDate,
                        recurrencePattern = recurrencePattern,
                        assignedTo = assignedTo,
                        createdBy = currentUserId
                    )
                )

            // Create notifications for chore creation
            try {
                if (assignedTo != null && assignedTo != currentUserId) {
                    @Serializable
                    data class NotificationParams(
                        @SerialName("user_id")
                        val userId: String,
                        @SerialName("house_id")
                        val houseId: String,
                        val title: String,
                        val message: String,
                        val type: String,
                        val data: String
                    )

                    supabase.postgrest.rpc(
                        function = "create_notification",
                        parameters = NotificationParams(
                            userId = assignedTo,
                            houseId = houseId,
                            title = "New Chore Assigned",
                            message = "You have been assigned a new chore: $taskName.",
                            type = "chore_assigned",
                            data = """{"type":"chore_assigned","taskName":"$taskName"}"""
                        )
                    )
                } else {
                    @Serializable
                    data class HouseNotificationParams(
                        @SerialName("p_house_id")
                        val houseId: String,
                        @SerialName("p_title")
                        val title: String,
                        @SerialName("p_message")
                        val message: String,
                        @SerialName("p_type")
                        val type: String,
                        @SerialName("p_data")
                        val data: String,
                        @SerialName("p_exclude_user_id")
                        val excludeUserId: String?
                    )

                    supabase.postgrest.rpc(
                        function = "create_notification_for_house",
                        parameters = HouseNotificationParams(
                            houseId = houseId,
                            title = "New Chore Created",
                            message = "New chore created: $taskName.",
                            type = "chore",
                            data = """{"type":"chore","taskName":"$taskName"}""",
                            excludeUserId = currentUserId
                        )
                    )
                }
            } catch (notificationError: Exception) {
                // Don't fail the whole operation if notification fails
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChore(
        choreId: String,
        taskName: String?,
        description: String?,
        dueDate: LocalDate?,
        assignedTo: String?
    ): Result<Unit> {
        return try {
            supabase.from("chores")
                .update(
                    ChoreUpdate(
                        taskName = taskName,
                        description = description,
                        dueDate = dueDate,
                        assignedTo = assignedTo
                    )
                ) {
                    filter {
                        eq("id", choreId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeChore(choreId: String, houseId: String, taskName: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("User not authenticated"))

            supabase.from("chores")
                .update(
                    ChoreUpdate(
                        isCompleted = true
                    )
                ) {
                    filter {
                        eq("id", choreId)
                    }
                }

            // Create notification for house
            try {
                @Serializable
                data class HouseNotificationParams(
                    @SerialName("p_house_id")
                    val houseId: String,
                    @SerialName("p_title")
                    val title: String,
                    @SerialName("p_message")
                    val message: String,
                    @SerialName("p_type")
                    val type: String,
                    @SerialName("p_data")
                    val data: String,
                    @SerialName("p_exclude_user_id")
                    val excludeUserId: String?
                )

                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = HouseNotificationParams(
                        houseId = houseId,
                        title = "Chore Completed",
                        message = "Completed the chore: $taskName.",
                        type = "chore",
                        data = """{"id":"$choreId","type":"chore"}""",
                        excludeUserId = currentUserId
                    )
                )
            } catch (e: Exception) {
                // Ignore notification errors
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChore(choreId: String): Result<Unit> {
        return try {
            supabase.from("chores")
                .delete {
                    filter {
                        eq("id", choreId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
