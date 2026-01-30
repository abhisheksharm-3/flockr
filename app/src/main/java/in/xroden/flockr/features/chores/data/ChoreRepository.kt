package `in`.xroden.flockr.features.chores.data

import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.network.RateLimiter
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.ChoreInsert
import `in`.xroden.flockr.data.dto.ChoreUpdate
import `in`.xroden.flockr.data.dto.HouseNotificationParams
import `in`.xroden.flockr.data.dto.NotificationParams
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.chores.model.ChoreWithProfiles
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock

@Singleton
class ChoreRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager,
    private val rateLimiter: RateLimiter
) : BaseRealtimeRepository(supabase, connectionManager), IChoreRepository {

    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override fun getCurrentUserId(): String? = userId

    override fun getChoresFlow(houseId: String): Flow<Result<List<Chore>>> {
        return createRealtimeFlow(
            channelId = "chores_$houseId",
            table = "chores",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getChores(houseId) }
        )
    }

    suspend fun getChores(houseId: String): Result<List<Chore>> {
        return try {
            val choresWithProfiles = supabase.from("chores")
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
                .decodeList<ChoreWithProfiles>()

            Result.success(choresWithProfiles.map { it.toChore() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createChore(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: LocalDate?,
        recurrencePattern: `in`.xroden.flockr.data.enums.ChoreRecurrence?,
        assignedTo: String?
    ): Result<Unit> {
        return rateLimiter.throttle("create_chore", maxRequestsPerMinute = 30) {
            try {
                val currentUserId = userId ?: return@throttle Result.failure(Exception("User not authenticated"))

                val validatedTaskName = Validators.validateChoreTask(taskName).getOrThrow()
                val sanitizedTaskName = InputSanitizer.sanitizeText(validatedTaskName)
                val sanitizedDescription = description?.trim()?.takeIf { it.isNotBlank() }
                    ?.let { InputSanitizer.sanitizeText(it) }

                supabase.from("chores")
                    .insert(
                        ChoreInsert(
                            houseId = houseId,
                            taskName = sanitizedTaskName,
                            description = sanitizedDescription,
                            dueDate = dueDate,
                            recurrencePattern = recurrencePattern,
                            assignedTo = assignedTo,
                            createdBy = currentUserId
                        )
                    )

                try {
                    if (assignedTo != null && assignedTo != currentUserId) {
                        supabase.postgrest.rpc(
                            function = "create_notification",
                            parameters = NotificationParams(
                                userId = assignedTo,
                                houseId = houseId,
                                title = "New Chore Assigned",
                                message = "You have been assigned a new chore: $sanitizedTaskName.",
                                type = "chore_assigned",
                                data = """{"type":"chore_assigned","taskName":"${sanitizedTaskName.replace("\"", "\\\"")}"}"""
                            )
                        )
                    } else {
                        supabase.postgrest.rpc(
                            function = "create_notification_for_house",
                            parameters = HouseNotificationParams(
                                houseId = houseId,
                                title = "New Chore Created",
                                message = "New chore created: $sanitizedTaskName.",
                                type = "chore",
                                data = """{"type":"chore","taskName":"${sanitizedTaskName.replace("\"", "\\\"")}"}""",
                                excludeUserId = currentUserId
                            )
                        )
                    }
                } catch (_: Exception) {
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
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

    override suspend fun completeChore(choreId: String, houseId: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("User not authenticated"))

            // Get task name for notification
            val chore = supabase.from("chores")
                .select {
                    filter { eq("id", choreId) }
                }
                .decodeSingleOrNull<Chore>()

            val taskName = chore?.taskName ?: "Chore"

            supabase.from("chores")
                .update(
                    ChoreUpdate(
                        isCompleted = true,
                        completedBy = currentUserId,
                        completedAt = Clock.System.now()
                    )
                ) {
                    filter {
                        eq("id", choreId)
                    }
                }

            // Create notification for house
            try {
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
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteChore(choreId: String, houseId: String): Result<Unit> {
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

    override suspend fun clearCompletedChores(houseId: String): Result<Unit> {
        return try {
            supabase.from("chores")
                .delete {
                    filter {
                        eq("house_id", houseId)
                        eq("is_completed", true)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
