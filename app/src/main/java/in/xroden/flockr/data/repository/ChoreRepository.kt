package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.Chore
import `in`.xroden.flockr.util.FlockrLogger
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
import kotlinx.coroutines.flow.map
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
            val chores = supabase.from("chores")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("due_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<Chore>()
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
        FlockrLogger.repoStart(TAG, "createChore", mapOf(
            "houseId" to houseId,
            "taskName" to taskName,
            "assignedTo" to assignedTo
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "createChore: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            val insertData = buildMap<String, Any> {
                put("house_id", houseId)
                put("task_name", taskName)
                description?.let { put("description", it) }
                dueDate?.let { put("due_date", it) }
                put("is_recurring", isRecurring)
                recurrencePattern?.let { put("recurrence_pattern", it) }
                assignedTo?.let { put("assigned_to", it) }
                put("created_by", currentUserId)
            }

            supabase.from("chores")
                .insert(insertData)

            // Create notification if assigned to someone
            if (assignedTo != null && assignedTo != currentUserId) {
                FlockrLogger.d(TAG, "createChore: Creating assignment notification")
                supabase.from("notifications")
                    .insert(
                        buildMap<String, Any> {
                            put("user_id", assignedTo)
                            put("house_id", houseId)
                            put("title", "New Chore Assigned")
                            put("message", "You have been assigned a new chore: $taskName.")
                            put("type", "chore")
                            put("data", mapOf("taskName" to taskName))
                        }
                    )
            }

            FlockrLogger.repoSuccess(TAG, "createChore", "Chore created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "createChore", e)
            Result.failure(e)
        }
    }

    suspend fun completeChore(choreId: String, houseId: String, taskName: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "completeChore", mapOf(
            "choreId" to choreId,
            "taskName" to taskName
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "completeChore: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            supabase.from("chores")
                .update(
                    mapOf(
                        "is_completed" to true,
                        "completed_by" to currentUserId,
                        "completed_at" to "now()"
                    )
                ) {
                    filter {
                        eq("id", choreId)
                    }
                }

            FlockrLogger.d(TAG, "completeChore: Creating completion notification")
            // Create notification for house
            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "Chore Completed",
                    "p_message" to "Completed the chore: $taskName.",
                    "p_type" to "chore",
                    "p_data" to mapOf("id" to choreId),
                    "p_exclude_user_id" to currentUserId
                )
            )

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

