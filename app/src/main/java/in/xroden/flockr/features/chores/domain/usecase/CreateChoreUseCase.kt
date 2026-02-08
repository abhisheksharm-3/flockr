package `in`.xroden.flockr.features.chores.domain.usecase

import `in`.xroden.flockr.data.enums.ChoreRecurrence
import `in`.xroden.flockr.features.chores.data.IChoreRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * Use case for creating chores with validation.
 * Encapsulates business rules for chore creation.
 */
class CreateChoreUseCase @Inject constructor(
    private val choreRepository: IChoreRepository
) {
    /**
     * Creates a new chore with proper validation.
     *
     * @param houseId The house where the chore belongs
     * @param taskName Name of the chore
     * @param description Optional description
     * @param dueDate Optional due date
     * @param recurrencePattern Optional recurrence pattern
     * @param assignedTo Optional user ID to assign to
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        houseId: String,
        taskName: String,
        description: String?,
        dueDate: LocalDate?,
        recurrencePattern: ChoreRecurrence?,
        assignedTo: String?
    ): Result<Unit> {
        // Validation: Task name should not be blank (already done by Validators, but double-check)
        if (taskName.isBlank()) {
            return Result.failure(IllegalArgumentException("Task name cannot be blank"))
        }

        // Validation: If recurrence is set, due date should also be set
        if (recurrencePattern != null && dueDate == null) {
            return Result.failure(IllegalArgumentException("Recurring chores must have a due date"))
        }

        return choreRepository.createChore(
            houseId = houseId,
            taskName = taskName,
            description = description,
            dueDate = dueDate,
            recurrencePattern = recurrencePattern,
            assignedTo = assignedTo
        )
    }
}
