package `in`.xroden.flockr.core.network

/**
 * Network error types for centralized error handling.
 */
sealed class NetworkError(override val message: String) : Exception(message) {
    data class NoConnection(override val message: String = "No internet connection") : NetworkError(message)
    data class Timeout(override val message: String = "Request timed out") : NetworkError(message)
    data class ServerError(val code: Int, override val message: String) : NetworkError(message)
    data class Unauthorized(override val message: String = "Unauthorized") : NetworkError(message)
    data class NotFound(override val message: String = "Resource not found") : NetworkError(message)
    data class Unknown(override val message: String = "Unknown error occurred") : NetworkError(message)
}

/**
 * Maps exceptions to NetworkError types for consistent error handling.
 */
object NetworkErrorMapper {
    fun mapError(throwable: Throwable): NetworkError {
        return when (throwable) {
            is java.net.UnknownHostException -> NetworkError.NoConnection()
            is java.net.SocketTimeoutException -> NetworkError.Timeout()
            is java.io.IOException -> NetworkError.NoConnection("Connection failed")
            else -> NetworkError.Unknown(throwable.message ?: "Unknown error")
        }
    }

    fun mapHttpError(statusCode: Int, message: String?): NetworkError {
        return when (statusCode) {
            401 -> NetworkError.Unauthorized()
            404 -> NetworkError.NotFound()
            in 500..599 -> NetworkError.ServerError(statusCode, message ?: "Server error")
            else -> NetworkError.Unknown(message ?: "HTTP error $statusCode")
        }
    }

    /**
     * Returns a user-friendly error message, sanitizing sensitive details.
     * Use this for all user-facing error messages.
     */
    fun getUserFriendlyMessage(throwable: Throwable): String {
        return when (throwable) {
            is NetworkError.NoConnection -> "No internet connection. Please check your network."
            is NetworkError.Timeout -> "Request timed out. Please try again."
            is NetworkError.ServerError -> "Server error. Please try again later."
            is NetworkError.Unauthorized -> "Session expired. Please sign in again."
            is NetworkError.NotFound -> "The requested item was not found."
            is NetworkError.Unknown -> "Something went wrong. Please try again."
            is java.net.UnknownHostException -> "No internet connection. Please check your network."
            is java.net.SocketTimeoutException -> "Request timed out. Please try again."
            is java.io.IOException -> "Connection failed. Please check your network."
            else -> "Something went wrong. Please try again."
        }
    }
}

