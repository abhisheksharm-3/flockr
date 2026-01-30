package `in`.xroden.flockr.features.chores.data

import `in`.xroden.flockr.data.enums.ChoreRecurrence
import `in`.xroden.flockr.features.chores.model.Chore
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository interface for chore management operations.
 * Enables easy mocking for unit tests.
 */
interface IChoreRepository {
    fun getChoresFlow(houseId: String): Flow<Result<List<Chore>>>
    suspend fun createChore(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: LocalDate?,
        recurrencePattern: ChoreRecurrence?,
        assignedTo: String?
    ): Result<Unit>
    suspend fun completeChore(choreId: String, houseId: String): Result<Unit>
    suspend fun deleteChore(choreId: String, houseId: String): Result<Unit>
    suspend fun clearCompletedChores(houseId: String): Result<Unit>
    fun getCurrentUserId(): String?
}
