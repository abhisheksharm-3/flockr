/** The failures the app raises itself, each with a message fit to show the user. */
package `in`.xroden.flockr.core.domain

sealed class DomainError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {

    sealed class AuthError(override val message: String) : DomainError(message) {
        object NotAuthenticated : AuthError("You're signed out. Sign in again to continue.")
    }

    sealed class ValidationError(override val message: String) : DomainError(message) {
        data class EmptyField(val fieldName: String) : ValidationError("$fieldName can't be empty")
        data class InvalidEmail(val email: String) : ValidationError("\"$email\" isn't an email address")
        data class InvalidLength(val fieldName: String, val min: Int, val max: Int) : ValidationError("$fieldName must be $min to $max characters")
        data class InvalidFormat(val fieldName: String, val expectedFormat: String) : ValidationError("$fieldName must be $expectedFormat")

        /** A rule the input breaks, stated as the sentence to show the user. */
        data class Rule(override val message: String) : ValidationError(message)
    }

    sealed class StorageError(override val message: String) : DomainError(message) {
        data class FileTooLarge(val size: Long, val maxSize: Long) : StorageError("That file is too large. The limit is ${maxSize / (1024 * 1024)} MB.")
        data class LimitReached(val limitType: String, val maxItems: Int) : StorageError("$limitType documents are limited to $maxItems")
    }
}

/** The signed-in user's id, or [DomainError.AuthError.NotAuthenticated] when there is none. */
fun requireAuthenticated(userId: String?): String = userId ?: throw DomainError.AuthError.NotAuthenticated
