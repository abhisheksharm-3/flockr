package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.Chore
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

    fun getChoresFlow(houseId: String): Flow<List<Chore>> {
        val channel = supabase.realtime.channel("chores_$houseId")

        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "chores"
            filter = "house_id=eq.$houseId"
        }.map {
            getChores(houseId)
        }
    }

    suspend fun getChores(houseId: String): List<Chore> {
        return try {
            supabase.from("chores")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("due_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<Chore>()
        } catch (e: Exception) {
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
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            supabase.from("chores")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "task_name" to taskName,
                        "description" to description,
                        "due_date" to dueDate,
                        "is_recurring" to isRecurring,
                        "recurrence_pattern" to recurrencePattern,
                        "assigned_to" to assignedTo,
                        "created_by" to currentUserId
                    )
                )

            // Create notification if assigned to someone
            if (assignedTo != null && assignedTo != currentUserId) {
                supabase.from("notifications")
                    .insert(
                        mapOf(
                            "user_id" to assignedTo,
                            "house_id" to houseId,
                            "title" to "New Chore Assigned",
                            "message" to "You have been assigned a new chore: $taskName.",
                            "data" to mapOf("type" to "chore", "taskName" to taskName)
                        )
                    )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeChore(choreId: String, houseId: String, taskName: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

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

            // Create notification for house
            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "Chore Completed",
                    "p_message" to "Completed the chore: $taskName.",
                    "p_data" to mapOf("type" to "chore", "id" to choreId),
                    "p_exclude_user_id" to currentUserId
                )
            )

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

