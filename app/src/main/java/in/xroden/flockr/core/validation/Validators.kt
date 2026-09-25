/** Checks on what a user types, before it reaches the database, so the message can name the field. */
package `in`.xroden.flockr.core.validation

import `in`.xroden.flockr.core.domain.DomainError

/** The length of a house invite code. Matches `generate_invite_code` in the schema. */
const val INVITE_CODE_LENGTH = 8

/** The characters an invite code is drawn from: capitals and digits without the look-alikes I, O, 0 and 1. */
private const val INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

/** The length limits match the check constraints on the same columns in the schema. */
object Validators {

    private val UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    /** The file types a document may be, which the document picker also offers. */
    val DOCUMENT_MIME_TYPES = setOf(
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

    /** The address trimmed and lower-cased, the form invitations are matched on. */
    fun validateEmail(email: String): Result<String> =
        if (email.trim().matches(EMAIL_REGEX)) Result.success(email.trim().lowercase())
        else Result.failure(DomainError.ValidationError.InvalidEmail(email))

    fun validateUUID(uuid: String, fieldName: String = "ID"): Result<String> =
        if (uuid.matches(UUID_REGEX)) Result.success(uuid)
        else Result.failure(DomainError.ValidationError.InvalidFormat(fieldName, "UUID format"))

    fun validateFileSize(size: Long, maxSize: Long): Result<Long> =
        if (size <= maxSize) Result.success(size) else Result.failure(DomainError.StorageError.FileTooLarge(size, maxSize))

    fun validateMimeType(mimeType: String): Result<String> =
        if (mimeType.lowercase() in DOCUMENT_MIME_TYPES) Result.success(mimeType)
        else Result.failure(DomainError.ValidationError.InvalidFormat("File", "a PDF, image, Office document or text file"))

    /** The code upper-cased and trimmed, when it could be one the database generated. */
    fun validateInviteCode(code: String): Result<String> {
        val cleanCode = code.trim().uppercase()
        return if (cleanCode.length == INVITE_CODE_LENGTH && cleanCode.all { it in INVITE_CODE_ALPHABET }) {
            Result.success(cleanCode)
        } else {
            Result.failure(DomainError.ValidationError.InvalidFormat("Invite code", "$INVITE_CODE_LENGTH letters and digits"))
        }
    }

    fun validateHouseName(name: String): Result<String> = validateText(name, "House name", 100)
    fun validateChoreTask(taskName: String): Result<String> = validateText(taskName, "Task name", 200)
    fun validateItemName(itemName: String): Result<String> = validateText(itemName, "Item name", 200)
    fun validateMessageContent(content: String): Result<String> = validateText(content, "Message", 4000)

    private fun validateText(value: String, fieldName: String, maxLength: Int): Result<String> {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> Result.failure(DomainError.ValidationError.EmptyField(fieldName))
            trimmed.length > maxLength -> Result.failure(DomainError.ValidationError.InvalidLength(fieldName, 1, maxLength))
            else -> Result.success(trimmed)
        }
    }
}
