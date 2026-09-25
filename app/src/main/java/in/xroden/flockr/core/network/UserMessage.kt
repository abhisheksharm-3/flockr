/** Turns a failed call into a sentence the app can show. */
package `in`.xroden.flockr.core.network

import `in`.xroden.flockr.core.domain.DomainError
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import java.io.IOException

/** The SQLSTATE every deliberate raise in the schema uses; its message is written for the user. */
private const val USER_FACING_SQLSTATE = "P0001"

private const val GENERIC_MESSAGE = "Something went wrong. Try again."
private const val OFFLINE_MESSAGE = "You're offline. Check your connection and try again."

/**
 * The message to show for this failure. A rule the database enforces arrives as its own sentence,
 * such as "This invite code is invalid or has expired", and sign-in failures carry the auth
 * server's own, such as "Invalid login credentials". Any other server error is reduced to a
 * generic line so table names and constraint text never reach the screen.
 */
fun Throwable.userMessage(): String = when (this) {
    is PostgrestRestException -> if (code == USER_FACING_SQLSTATE) error else GENERIC_MESSAGE
    is AuthRestException -> errorDescription
    is DomainError -> message
    is HttpRequestException, is IOException -> OFFLINE_MESSAGE
    else -> GENERIC_MESSAGE
}
