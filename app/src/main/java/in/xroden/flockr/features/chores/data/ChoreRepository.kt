/** A house's chores. Scheduling the next turn of a repeating chore and notifying people happen in the database. */
package `in`.xroden.flockr.features.chores.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.chores.model.ChoreRecurrence
import `in`.xroden.flockr.core.realtime.TableWatch
import `in`.xroden.flockr.core.realtime.liveQuery
import `in`.xroden.flockr.core.serialization.InstantSerializer
import `in`.xroden.flockr.core.serialization.LocalDateSerializer
import `in`.xroden.flockr.features.chores.model.Chore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The chore fields a member writes when adding or editing one. */
@Serializable
data class ChoreDraft(
    @SerialName("task_name") val taskName: String,
    val description: String?,
    @SerialName("due_date") @Serializable(with = LocalDateSerializer::class) val dueDate: LocalDate?,
    @SerialName("recurrence_pattern") val recurrencePattern: ChoreRecurrence?,
    val rotation: List<String>,
    @SerialName("effort_points") val effortPoints: Int,
    @SerialName("assigned_to") val assignedTo: String?,
)

@Serializable
private data class ChoreInsert(
    @SerialName("house_id") val houseId: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("task_name") val taskName: String,
    val description: String?,
    @SerialName("due_date") @Serializable(with = LocalDateSerializer::class) val dueDate: LocalDate?,
    @SerialName("recurrence_pattern") val recurrencePattern: ChoreRecurrence?,
    val rotation: List<String>,
    @SerialName("effort_points") val effortPoints: Int,
    @SerialName("assigned_to") val assignedTo: String?,
)

@Serializable
private data class ChoreCompletion(
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("completed_at") @Serializable(with = InstantSerializer::class) val completedAt: Instant?,
    @SerialName("completed_by") val completedBy: String?,
)

@Singleton
class ChoreRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Every chore in the house, open ones soonest due first, kept current. */
    fun getChoresFlow(houseId: String): Flow<Result<List<Chore>>> =
        supabase.liveQuery(listOf(TableWatch("chores", "house_id", houseId))) {
            supabase.from("chores").select {
                filter { eq("house_id", houseId) }
                order("is_completed", Order.ASCENDING)
                order("due_date", Order.ASCENDING, nullsFirst = false)
                limit(300)
            }.decodeList<Chore>()
        }

    suspend fun getChore(choreId: String): Result<Chore> = runCatching {
        supabase.from("chores").select { filter { eq("id", choreId) } }.decodeSingle<Chore>()
    }

    suspend fun createChore(houseId: String, draft: ChoreDraft): Result<Unit> = runCatching {
        val userId = requireAuthenticated(getCurrentUserId())
        val clean = draft.sanitized()
        supabase.from("chores").insert(
            ChoreInsert(houseId, userId, clean.taskName, clean.description, clean.dueDate, clean.recurrencePattern, clean.rotation, clean.effortPoints, clean.assignedTo)
        )
    }

    suspend fun updateChore(choreId: String, draft: ChoreDraft): Result<Unit> = runCatching {
        supabase.from("chores").update(draft.sanitized()) { filter { eq("id", choreId) } }
    }

    /** Ticks the chore off, or back on; a repeating chore's next turn is scheduled once, on the first tick. */
    suspend fun setCompleted(choreId: String, isCompleted: Boolean): Result<Unit> = runCatching {
        val userId = requireAuthenticated(getCurrentUserId())
        val completion = if (isCompleted) ChoreCompletion(true, Clock.System.now(), userId) else ChoreCompletion(false, null, null)
        supabase.from("chores").update(completion) { filter { eq("id", choreId) } }
    }

    suspend fun deleteChore(choreId: String): Result<Unit> = runCatching {
        supabase.from("chores").delete { filter { eq("id", choreId) } }
    }

    suspend fun clearCompletedChores(houseId: String): Result<Unit> = runCatching {
        supabase.from("chores").delete {
            filter {
                eq("house_id", houseId)
                eq("is_completed", true)
            }
        }
    }

    private fun ChoreDraft.sanitized() = copy(
        taskName = InputSanitizer.sanitizeText(Validators.validateChoreTask(taskName).getOrThrow()),
        description = description?.let(InputSanitizer::sanitizeText)?.ifBlank { null },
        rotation = if (recurrencePattern == null) emptyList() else rotation,
    )
}
