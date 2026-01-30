package `in`.xroden.flockr.core.validation

import `in`.xroden.flockr.core.domain.DomainError

typealias ValidationResult<T> = Result<T>

object Validators {

    fun validateNonEmpty(value: String, fieldName: String): ValidationResult<String> {
        return if (value.isBlank()) {
            Result.failure(DomainError.ValidationError.EmptyField(fieldName))
        } else {
            Result.success(value.trim())
        }
    }

    fun validateEmail(email: String): ValidationResult<String> {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return if (email.matches(emailRegex)) {
            Result.success(email.trim().lowercase())
        } else {
            Result.failure(DomainError.ValidationError.InvalidEmail(email))
        }
    }

    fun validateAmount(amount: String): ValidationResult<java.math.BigDecimal> {
        return try {
            val decimal = java.math.BigDecimal(amount)
            if (decimal < java.math.BigDecimal.ZERO) {
                Result.failure(DomainError.ExpenseError.InvalidAmount(amount))
            } else {
                Result.success(decimal)
            }
        } catch (e: NumberFormatException) {
            Result.failure(DomainError.ExpenseError.InvalidAmount(amount))
        }
    }

    fun validatePositiveAmount(amount: java.math.BigDecimal, fieldName: String = "Amount"): ValidationResult<java.math.BigDecimal> {
        return if (amount <= java.math.BigDecimal.ZERO) {
            Result.failure(DomainError.ValidationError.InvalidFormat(fieldName, "positive number"))
        } else {
            Result.success(amount)
        }
    }

    fun validateLength(value: String, fieldName: String, min: Int, max: Int): ValidationResult<String> {
        val trimmed = value.trim()
        return if (trimmed.length in min..max) {
            Result.success(trimmed)
        } else {
            Result.failure(DomainError.ValidationError.InvalidLength(fieldName, min, max))
        }
    }

    fun validateDate(dateString: String): ValidationResult<kotlinx.datetime.LocalDate> {
        return try {
            val date = kotlinx.datetime.LocalDate.parse(dateString)
            Result.success(date)
        } catch (e: Exception) {
            Result.failure(DomainError.ValidationError.InvalidFormat("Date", "ISO date format (YYYY-MM-DD)"))
        }
    }

    fun validateInviteCode(code: String): ValidationResult<String> {
        val cleanCode = code.trim().uppercase()
        return if (cleanCode.matches("^[A-Z0-9]{6}$".toRegex())) {
            Result.success(cleanCode)
        } else {
            Result.failure(DomainError.ValidationError.InvalidFormat("Invite code", "6 alphanumeric characters"))
        }
    }

    fun validateHouseName(name: String): ValidationResult<String> {
        return validateNonEmpty(name, "House name")
            .flatMap { validateLength(it, "House name", 1, 100) }
    }

    fun validateChoreTask(taskName: String): ValidationResult<String> {
        return validateNonEmpty(taskName, "Task name")
            .flatMap { validateLength(it, "Task name", 1, 200) }
    }

    fun validateItemName(itemName: String): ValidationResult<String> {
        return validateNonEmpty(itemName, "Item name")
            .flatMap { validateLength(it, "Item name", 1, 100) }
    }

    fun validateMessageContent(content: String): ValidationResult<String> {
        return validateNonEmpty(content, "Message")
            .flatMap { validateLength(it, "Message", 1, 2000) }
    }

    fun <T> Result<T>.flatMap(transform: (T) -> Result<T>): Result<T> {
        return fold(
            onSuccess = { transform(it) },
            onFailure = { Result.failure(it) }
        )
    }
}
