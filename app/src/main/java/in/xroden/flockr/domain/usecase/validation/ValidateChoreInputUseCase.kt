package `in`.xroden.flockr.domain.usecase.validation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
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
    operator fun invoke(
        taskName: String,
        dueDate: LocalDate?
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate task name
        if (taskName.isBlank()) {
            errors.add("Task name is required")
        } else if (taskName.length < 2) {
            errors.add("Task name must be at least 2 characters")
        } else if (taskName.length > 100) {
            errors.add("Task name must be less than 100 characters")
        }

        // Validate due date (if provided)
        if (dueDate != null) {
            val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
            val yesterday = today.minus(kotlinx.datetime.DateTimeUnit.DayBased(1))
            if (dueDate < yesterday) {
                errors.add("Due date cannot be in the past")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    sealed interface ValidationResult {
        data object Success : ValidationResult
        data class Error(val errors: List<String>) : ValidationResult
    }
}


