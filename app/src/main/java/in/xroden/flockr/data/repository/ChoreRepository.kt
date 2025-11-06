package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.Chore
import `in`.xroden.flockr.data.model.CreateNotificationParams
import `in`.xroden.flockr.utils.FlockrLogger
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
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChoreRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id
    
    companion object {
        private const val TAG = "ChoreRepository"
    }

    fun getChoresFlow(houseId: String): Flow<List<Chore>> {
        FlockrLogger.realtimeEvent(TAG, "getChoresFlow", "Starting for house=$houseId")
        return kotlinx.coroutines.flow.flow {
            // Emit initial value immediately
            val initialChores = getChores(houseId)
            FlockrLogger.d(TAG, "getChoresFlow: Emitting initial ${initialChores.size} chores")
            emit(initialChores)

            // Then listen for realtime updates
            val channelId = "chores_${houseId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "chores"
                }
                
                FlockrLogger.realtimeEvent(TAG, "getChoresFlow", "Subscribing to channel $channelId")
                channel.subscribe(blockUntilSubscribed = true)
                FlockrLogger.realtimeEvent(TAG, "getChoresFlow", "Successfully subscribed")

                changeFlow.collect { action ->
                    FlockrLogger.realtimeEvent(TAG, "getChoresFlow", "Received update: $action")
                    // Small delay to ensure database consistency
                    kotlinx.coroutines.delay(100)
                    val updatedChores = getChores(houseId)
                    FlockrLogger.d(TAG, "getChoresFlow: Emitting ${updatedChores.size} chores after update")
                    emit(updatedChores)
                }
            } catch (e: Exception) {
                FlockrLogger.repoError(TAG, "getChoresFlow", e)
            } finally {
                try {
                    FlockrLogger.d(TAG, "getChoresFlow: Cleaning up channel")
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    FlockrLogger.e(TAG, "getChoresFlow: Error removing channel", e)
                }
            }
        }
    }

    suspend fun getChores(houseId: String): List<Chore> {
        FlockrLogger.repoStart(TAG, "getChores", mapOf("houseId" to houseId))
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
                    dueDate = obj["due_date"]?.jsonPrimitive?.contentOrNull,
                    isCompleted = obj["is_completed"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                    completedAt = obj["completed_at"]?.jsonPrimitive?.contentOrNull,
                    completedBy = obj["completed_by"]?.jsonPrimitive?.contentOrNull,
                    completedByName = completedByName,
                    recurrencePattern = obj["recurrence_pattern"]?.jsonPrimitive?.contentOrNull,
                    createdBy = obj["created_by"]?.jsonPrimitive?.contentOrNull,
                    createdByName = createdByName,
                    createdAt = obj["created_at"]?.jsonPrimitive?.content ?: ""
                )
            }
            
            FlockrLogger.repoSuccess(TAG, "getChores", "Found ${chores.size} chores")
            chores
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getChores", e)
            emptyList()
        }
    }

    suspend fun createChore(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: String?,
        isRecurring: Boolean,
        recurrencePattern: String?,
        assignedTo: String?
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "createChore", mapOf("houseId" to houseId, "taskName" to taskName))
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("User not authenticated"))

            FlockrLogger.d(TAG, "createChore: Inserting chore into database")

            @kotlinx.serialization.Serializable
            data class ChoreInsert(
                @kotlinx.serialization.SerialName("house_id")
                val houseId: String,
                @kotlinx.serialization.SerialName("task_name")
                val taskName: String,
                @kotlinx.serialization.SerialName("description")
                val description: String? = null,
                @kotlinx.serialization.SerialName("due_date")
                val dueDate: String? = null,
                @kotlinx.serialization.SerialName("recurrence_pattern")
                val recurrencePattern: String? = null,
                @kotlinx.serialization.SerialName("assigned_to")
                val assignedTo: String? = null,
                @kotlinx.serialization.SerialName("created_by")
                val createdBy: String
            )

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

            // Create notification if assigned to someone
            if (assignedTo != null && assignedTo != currentUserId) {
                FlockrLogger.d(TAG, "createChore: Creating assignment notification")

                @kotlinx.serialization.Serializable
                data class NotificationParams(
                    @kotlinx.serialization.SerialName("user_id")
                    val userId: String,
                    @kotlinx.serialization.SerialName("house_id")
                    val houseId: String,
                    @kotlinx.serialization.SerialName("title")
                    val title: String,
                    @kotlinx.serialization.SerialName("message")
                    val message: String,
                    @kotlinx.serialization.SerialName("type")
                    val type: String,
                    @kotlinx.serialization.SerialName("data")
                    val data: String
                )

                supabase.postgrest.rpc(
                    function = "create_notification",
                    parameters = NotificationParams(
                        userId = assignedTo,
                        houseId = houseId,
                        title = "New Chore Assigned",
                        message = "You have been assigned a new chore: $taskName.",
                        type = "chore",
                        data = """{"taskName":"$taskName"}"""
                    )
                ).decodeAs<Unit>()
            }

            FlockrLogger.repoSuccess(TAG, "createChore", "Chore created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "createChore", e)
            Result.failure(e)
        }
    }

    suspend fun updateChore(
        choreId: String,
        taskName: String,
        description: String?,
        dueDate: String?,
        assignedTo: String?
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "updateChore", mapOf("choreId" to choreId, "taskName" to taskName))
        return try {
            @kotlinx.serialization.Serializable
            data class ChoreUpdate(
                @kotlinx.serialization.SerialName("task_name")
                val taskName: String,
                @kotlinx.serialization.SerialName("description")
                val description: String?,
                @kotlinx.serialization.SerialName("due_date")
                val dueDate: String?,
                @kotlinx.serialization.SerialName("assigned_to")
                val assignedTo: String?
            )

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

            FlockrLogger.repoSuccess(TAG, "updateChore", "Chore updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "updateChore", e)
            Result.failure(e)
        }
    }

    suspend fun completeChore(choreId: String, houseId: String, taskName: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "completeChore", mapOf("choreId" to choreId))
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("User not authenticated"))

            FlockrLogger.d(TAG, "completeChore: Marking chore as completed")

            @kotlinx.serialization.Serializable
            data class ChoreUpdate(
                @kotlinx.serialization.SerialName("is_completed")
                val isCompleted: Boolean,
                @kotlinx.serialization.SerialName("completed_by")
                val completedBy: String,
                @kotlinx.serialization.SerialName("completed_at")
                val completedAt: String
            )

            supabase.from("chores")
                .update(
                    ChoreUpdate(
                        isCompleted = true,
                        completedBy = currentUserId,
                        completedAt = "now()"
                    )
                ) {
                    filter {
                        eq("id", choreId)
                    }
                }

            FlockrLogger.d(TAG, "completeChore: Creating completion notification")
            // Create notification for house
            val notificationParams = CreateNotificationParams(
                houseId = houseId,
                title = "Chore Completed",
                message = "Completed the chore: $taskName.",
                data = """{"id":"$choreId","type":"chore"}""",
                excludeUserId = currentUserId
            )
            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = notificationParams
            ).decodeAs<Unit>()

            FlockrLogger.repoSuccess(TAG, "completeChore", "Chore marked as completed")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "completeChore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteChore(choreId: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "deleteChore", mapOf("choreId" to choreId))
        return try {
            supabase.from("chores")
                .delete {
                    filter {
                        eq("id", choreId)
                    }
                }

            FlockrLogger.repoSuccess(TAG, "deleteChore", "Chore deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "deleteChore", e)
            Result.failure(e)
        }
    }
}

