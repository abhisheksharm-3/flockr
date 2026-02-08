package `in`.xroden.flockr.core.validation

import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.domain.flatMap

typealias ValidationResult<T> = Result<T>

/** Centralized validation utilities for input validation. */
object Validators {

    private val UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val INVITE_CODE_REGEX = "^[A-Z0-9]{6}$".toRegex()

    private val ALLOWED_DOCUMENT_MIME_TYPES = setOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    )

    fun validateNonEmpty(value: String, fieldName: String): ValidationResult<String> =
        if (value.isBlank()) {
            Result.failure(DomainError.ValidationError.EmptyField(fieldName))
        } else {
            Result.success(value.trim())
        }

    fun validateEmail(email: String): ValidationResult<String> =
        if (email.matches(EMAIL_REGEX)) {
            Result.success(email.trim().lowercase())
        } else {
            Result.failure(DomainError.ValidationError.InvalidEmail(email))
        }

    fun validateUUID(uuid: String, fieldName: String = "ID"): ValidationResult<String> =
        if (uuid.matches(UUID_REGEX)) {
            Result.success(uuid)
        } else {
            Result.failure(DomainError.ValidationError.InvalidFormat(fieldName, "UUID format"))
        }

    fun validateFileSize(size: Long, maxSize: Long, fieldName: String = "File"): ValidationResult<Long> =
        if (size <= maxSize) {
            Result.success(size)
        } else {
            Result.failure(DomainError.StorageError.FileTooLarge(size, maxSize))
        }

    fun validateMimeType(mimeType: String): ValidationResult<String> =
        if (ALLOWED_DOCUMENT_MIME_TYPES.contains(mimeType.lowercase())) {
            Result.success(mimeType)
        } else {
            Result.failure(DomainError.DocumentError.InvalidMimeType(mimeType))
        }

    fun validateAmount(amount: String): ValidationResult<java.math.BigDecimal> = try {
        val decimal = java.math.BigDecimal(amount)
        if (decimal < java.math.BigDecimal.ZERO) {
            Result.failure(DomainError.ExpenseError.InvalidAmount(amount))
        } else {
            Result.success(decimal)
        }
    } catch (e: NumberFormatException) {
        Result.failure(DomainError.ExpenseError.InvalidAmount(amount))
    }

    fun validatePositiveAmount(amount: java.math.BigDecimal, fieldName: String = "Amount"): ValidationResult<java.math.BigDecimal> =
        if (amount <= java.math.BigDecimal.ZERO) {
            Result.failure(DomainError.ValidationError.InvalidFormat(fieldName, "positive number"))
        } else {
            Result.success(amount)
        }

    fun validateLength(value: String, fieldName: String, min: Int, max: Int): ValidationResult<String> {
        val trimmed = value.trim()
        return if (trimmed.length in min..max) {
            Result.success(trimmed)
        } else {
            Result.failure(DomainError.ValidationError.InvalidLength(fieldName, min, max))
        }
    }

    fun validateDate(dateString: String): ValidationResult<kotlinx.datetime.LocalDate> = try {
        Result.success(kotlinx.datetime.LocalDate.parse(dateString))
    } catch (e: Exception) {
        Result.failure(DomainError.ValidationError.InvalidFormat("Date", "ISO date format (YYYY-MM-DD)"))
    }

    fun validateInviteCode(code: String): ValidationResult<String> {
        val cleanCode = code.trim().uppercase()
        return if (cleanCode.matches("^[A-Z0-9]{6}$".toRegex())) {
            Result.success(cleanCode)
        } else {
            Result.failure(DomainError.ValidationError.InvalidFormat("Invite code", "6 alphanumeric characters"))
        }
    }

    fun validateHouseName(name: String): ValidationResult<String> =
        validateNonEmpty(name, "House name").flatMap { validateLength(it, "House name", 1, 100) }

    fun validateChoreTask(taskName: String): ValidationResult<String> =
        validateNonEmpty(taskName, "Task name").flatMap { validateLength(it, "Task name", 1, 200) }

    fun validateItemName(itemName: String): ValidationResult<String> =
        validateNonEmpty(itemName, "Item name").flatMap { validateLength(it, "Item name", 1, 100) }

    fun validateMessageContent(content: String): ValidationResult<String> =
        validateNonEmpty(content, "Message").flatMap { validateLength(it, "Message", 1, 2000) }
}
