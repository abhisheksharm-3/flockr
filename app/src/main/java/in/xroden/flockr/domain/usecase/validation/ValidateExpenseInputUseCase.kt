package `in`.xroden.flockr.domain.usecase.validation

import java.math.BigDecimal
import javax.inject.Inject

/**
 * Use case to validate expense input data
 */
class ValidateExpenseInputUseCase @Inject constructor() {

    /**
     * Validate expense input data
     * 
     * @return Result with Unit on success, or error message on failure
     */
    operator fun invoke(
        name: String,
        amount: BigDecimal?,
        category: String
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate name
        if (name.isBlank()) {
            errors.add("Expense name is required")
        } else if (name.length < 2) {
            errors.add("Expense name must be at least 2 characters")
        } else if (name.length > 100) {
            errors.add("Expense name must be less than 100 characters")
        }

        // Validate amount
        if (amount == null) {
            errors.add("Amount is required")
        } else {
            if (amount <= BigDecimal.ZERO) {
                errors.add("Amount must be greater than zero")
            }
            if (amount > BigDecimal("999999.99")) {
                errors.add("Amount is too large")
            }
        }

        // Validate category
        if (category.isBlank()) {
            errors.add("Category is required")
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


