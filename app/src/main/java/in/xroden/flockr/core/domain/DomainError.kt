package `in`.xroden.flockr.core.domain

/** Base sealed class for all domain errors. Extends Exception for Result.failure() compatibility. */
sealed class DomainError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {

    sealed class AuthError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        object NotAuthenticated : AuthError("User is not authenticated")
        data class InvalidCredentials(override val cause: Throwable? = null) : AuthError("Invalid email or password", cause)
        data class SignUpFailed(override val message: String, override val cause: Throwable? = null) : AuthError(message, cause)
        data class SignInFailed(override val message: String, override val cause: Throwable? = null) : AuthError(message, cause)
        data class ProfileLoadFailed(override val cause: Throwable? = null) : AuthError("Failed to load profile", cause)
    }

    sealed class HouseError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        data class CreationFailed(override val message: String, override val cause: Throwable? = null) : HouseError(message, cause)
        data class NotFound(val houseId: String) : HouseError("House not found: $houseId")
        data class InvalidInviteCode(val code: String) : HouseError("Invalid invite code: $code")
        data class JoinFailed(override val message: String, override val cause: Throwable? = null) : HouseError(message, cause)
        data class LoadFailed(override val cause: Throwable? = null) : HouseError("Failed to load houses", cause)
        data class UpdateFailed(override val cause: Throwable? = null) : HouseError("Failed to update house", cause)
    }

    sealed class ExpenseError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        data class CreationFailed(override val message: String, override val cause: Throwable? = null) : ExpenseError(message, cause)
        data class LoadFailed(override val cause: Throwable? = null) : ExpenseError("Failed to load expenses", cause)
        data class UpdateFailed(override val cause: Throwable? = null) : ExpenseError("Failed to update expense", cause)
        data class DeleteFailed(override val cause: Throwable? = null) : ExpenseError("Failed to delete expense", cause)
        data class InvalidAmount(val amount: String) : ExpenseError("Invalid amount: $amount")
        data class BalanceLoadFailed(override val cause: Throwable? = null) : ExpenseError("Failed to load balances", cause)
    }

    sealed class ValidationError(override val message: String) : DomainError(message) {
        data class EmptyField(val fieldName: String) : ValidationError("$fieldName cannot be empty")
        data class InvalidEmail(val email: String) : ValidationError("Invalid email format: $email")
        data class InvalidLength(val fieldName: String, val min: Int, val max: Int) : ValidationError("$fieldName must be between $min and $max characters")
        data class InvalidFormat(val fieldName: String, val expectedFormat: String) : ValidationError("$fieldName has invalid format. Expected: $expectedFormat")
    }

    sealed class NetworkError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        object NoConnection : NetworkError("No internet connection")
        data class Timeout(override val cause: Throwable? = null) : NetworkError("Request timed out", cause)
        data class ServerError(val code: Int, override val message: String) : NetworkError(message)
        data class Unknown(override val cause: Throwable) : NetworkError("Network error occurred", cause)
    }

    sealed class StorageError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        data class UploadFailed(override val message: String, override val cause: Throwable? = null) : StorageError(message, cause)
        data class DownloadFailed(override val cause: Throwable? = null) : StorageError("Failed to download file", cause)
        data class DeleteFailed(override val cause: Throwable? = null) : StorageError("Failed to delete file", cause)
        data class FileTooLarge(val size: Long, val maxSize: Long) : StorageError("File size $size exceeds maximum $maxSize bytes")
        data class LimitReached(val limitType: String, val maxItems: Int) : StorageError("$limitType document limit reached (max $maxItems)")
    }

    sealed class ChatError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        data class SendFailed(override val cause: Throwable? = null) : ChatError("Failed to send message", cause)
        data class LoadFailed(override val cause: Throwable? = null) : ChatError("Failed to load messages", cause)
    }

    sealed class DocumentError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        data class UploadFailed(override val message: String, override val cause: Throwable? = null) : DocumentError(message, cause)
        data class DeleteFailed(override val cause: Throwable? = null) : DocumentError("Failed to delete document", cause)
        data class LoadFailed(override val cause: Throwable? = null) : DocumentError("Failed to load documents", cause)
        data class InvalidMimeType(val mimeType: String) : DocumentError("Invalid file type: $mimeType")
    }

    sealed class ShoppingError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        data class AddFailed(override val cause: Throwable? = null) : ShoppingError("Failed to add item", cause)
        data class UpdateFailed(override val cause: Throwable? = null) : ShoppingError("Failed to update item", cause)
        data class DeleteFailed(override val cause: Throwable? = null) : ShoppingError("Failed to delete item", cause)
        data class LoadFailed(override val cause: Throwable? = null) : ShoppingError("Failed to load shopping list", cause)
    }

    sealed class ChoreError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause) {
        data class CreateFailed(override val cause: Throwable? = null) : ChoreError("Failed to create chore", cause)
        data class UpdateFailed(override val cause: Throwable? = null) : ChoreError("Failed to update chore", cause)
        data class DeleteFailed(override val cause: Throwable? = null) : ChoreError("Failed to delete chore", cause)
        data class LoadFailed(override val cause: Throwable? = null) : ChoreError("Failed to load chores", cause)
        data class CompleteFailed(override val cause: Throwable? = null) : ChoreError("Failed to complete chore", cause)
    }

    data class UnknownError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause)
}

typealias DomainResult<T> = Result<T>

/** Converts throwable to typed DomainError. */
fun Throwable.toDomainError(): DomainError = when (this) {
    is DomainError -> this
    is IllegalStateException -> when {
        message?.contains("not authenticated", ignoreCase = true) == true -> DomainError.AuthError.NotAuthenticated
        message?.contains("no user logged in", ignoreCase = true) == true -> DomainError.AuthError.NotAuthenticated
        else -> DomainError.UnknownError(message ?: "Unknown error", this)
    }
    is IllegalArgumentException -> DomainError.ValidationError.InvalidFormat("Input", message ?: "Invalid input")
    else -> DomainError.UnknownError(message ?: "Unknown error occurred", this)
}

/** Requires authenticated user or throws NotAuthenticated error. */
fun requireAuthenticated(userId: String?): String =
    userId ?: throw DomainError.AuthError.NotAuthenticated

/** Maps Result failure to a different error type. */
inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) }
    )

/** Executes onSuccess block as a suspend function. */
suspend inline fun <T, R> Result<T>.mapSuspend(crossinline transform: suspend (T) -> R): Result<R> =
    fold(
        onSuccess = { runCatching { transform(it) } },
        onFailure = { Result.failure(it) }
    )

/** FlatMaps Result, allowing chained operations that return Result. */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )
