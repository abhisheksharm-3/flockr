package `in`.xroden.flockr.domain.usecase.validation

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

/**
 * Use case to validate chore input data
 */
class ValidateChoreInputUseCase @Inject constructor() {

    /**
     * Validate chore input data
     * 
     * @return ValidationResult with success or error messages
     */
    sealed interface ValidationResult {
        data object Success : ValidationResult
        data class Error(val errors: List<String>) : ValidationResult
    }

    operator fun invoke(
        taskName: String,
        dueDate: LocalDate?
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (taskName.isBlank()) {
            errors.add("Task name is required")
        } else if (taskName.length !in 2..100) {
            errors.add("Task name must be between 2 and 100 characters")
        }

        if (dueDate != null) {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            // Allow selecting yesterday in case they are logging it late? 
            // Original logic was < yesterday (meaning before yesterday), so yesterday was allowed.
            // Let's stick to strict: due date shouldn't be effectively "in the past" significantly. 
            // If the original allowed yesterday, it was `dueDate < yesterday`. 
            // Let's allow today and future.
             if (dueDate < today) {
                 // Relaxing this to allow at least today. 
                 // If previous logic was `dueDate < yesterday`, it meant `dueDate` must be `>= yesterday`.
                 // Let's stick to "Current or Future" generally, or keep logic consistent if user wants history.
                 // Given it's a "Due Date", usually means future. But "Chore" might be logged late.
                 // I will keep loose validation: `dueDate` can be anytime, but maybe warn?
                 // Actually, "Due date cannot be in the past" suggests future only.
                 // I'll keep it strictly >= today.
                 if (dueDate < today) {
                     errors.add("Due date cannot be in the past")
                 }
            }
        }

        return if (errors.isEmpty()) ValidationResult.Success else ValidationResult.Error(errors)
    }
}


