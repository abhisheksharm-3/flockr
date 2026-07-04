package `in`.xroden.flockr.features.chores.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.network.RateLimiter
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.notification.NotificationService
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.ChoreInsert
import `in`.xroden.flockr.data.dto.ChoreUpdate
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.chores.model.ChoreWithProfiles
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import `in`.xroden.flockr.data.enums.ChoreRecurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class ChoreRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager,
    private val rateLimiter: RateLimiter,
    private val notificationService: NotificationService
) : BaseRealtimeRepository(supabase, connectionManager), IChoreRepository {

    override fun getCurrentUserId(): String? = authenticatedUserId

    override fun getChoresFlow(houseId: String): Flow<Result<List<Chore>>> =
        createRealtimeFlow(
            channelId = "chores_$houseId",
            table = "chores",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getChores(houseId) }
        )

    private suspend fun getChores(houseId: String): Result<List<Chore>> = runCatching {
        supabase.from("chores")
            .select(Columns.raw("""
                *,
                assigned_to_profile:profiles!chores_assigned_to_fkey(full_name),
                completed_by_profile:profiles!chores_completed_by_fkey(full_name),
                created_by_profile:profiles!chores_created_by_fkey(full_name)
            """.trimIndent())) {
                filter { eq("house_id", houseId) }
                order("due_date", Order.ASCENDING)
            }
            .decodeList<ChoreWithProfiles>()
            .map { it.toChore() }
    }

    override suspend fun createChore(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: LocalDate?,
        recurrencePattern: `in`.xroden.flockr.data.enums.ChoreRecurrence?,
        assignedTo: String?
    ): Result<Unit> = rateLimiter.throttle("create_chore", maxRequestsPerMinute = 30) {
        runCatching {
            val userId = requireAuthenticated(authenticatedUserId)
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
                        createdBy = userId
                    )
                )

            if (assignedTo != null && assignedTo != userId) {
                notificationService.sendChoreAssigned(houseId, assignedTo, sanitizedTaskName, userId)
            } else {
                notificationService.sendChoreCreated(houseId, sanitizedTaskName, userId)
            }
        }
    }

    override suspend fun updateChore(
        choreId: String,
        taskName: String?,
        description: String?,
        dueDate: LocalDate?,
        assignedTo: String?
    ): Result<Unit> = runCatching {
        val sanitizedTaskName = taskName?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedDescription = description?.let { InputSanitizer.sanitizeText(it) }

        supabase.from("chores")
            .update(ChoreUpdate(
                taskName = sanitizedTaskName,
                description = sanitizedDescription,
                dueDate = dueDate,
                assignedTo = assignedTo
            )) {
                filter { eq("id", choreId) }
            }
    }

    override suspend fun completeChore(choreId: String, houseId: String): Result<Unit> = runCatching {
        val userId = requireAuthenticated(authenticatedUserId)
        val chore = supabase.from("chores")
            .select { filter { eq("id", choreId) } }
            .decodeSingleOrNull<Chore>()
        val taskName = chore?.taskName ?: "Chore"

        supabase.from("chores")
            .update(ChoreUpdate(isCompleted = true, completedBy = userId, completedAt = Clock.System.now())) {
                filter { eq("id", choreId) }
            }

        // Recurring chore: schedule the next occurrence from the current due date.
        val pattern = chore?.recurrencePattern
        val currentDue = chore?.dueDate
        if (chore != null && pattern != null && currentDue != null) {
            val nextDue = when (pattern) {
                ChoreRecurrence.DAILY -> currentDue.plus(DatePeriod(days = 1))
                ChoreRecurrence.WEEKLY -> currentDue.plus(DatePeriod(days = 7))
                ChoreRecurrence.MONTHLY -> currentDue.plus(DatePeriod(months = 1))
                ChoreRecurrence.YEARLY -> currentDue.plus(DatePeriod(years = 1))
            }
            supabase.from("chores").insert(
                ChoreInsert(
                    houseId = houseId,
                    taskName = chore.taskName,
                    description = chore.description,
                    dueDate = nextDue,
                    recurrencePattern = pattern,
                    assignedTo = chore.assignedTo,
                    createdBy = chore.createdBy ?: userId
                )
            )
        }

        notificationService.sendChoreCompleted(houseId, choreId, taskName, userId)
    }

    override suspend fun deleteChore(choreId: String, houseId: String): Result<Unit> = runCatching {
        supabase.from("chores").delete { filter { eq("id", choreId) } }
    }

    override suspend fun clearCompletedChores(houseId: String): Result<Unit> = runCatching {
        supabase.from("chores").delete {
            filter {
                eq("house_id", houseId)
                eq("is_completed", true)
            }
        }
    }
}
