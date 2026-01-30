package `in`.xroden.flockr.core.ui.pagination

/**
 * Pagination configuration for lazy loading large lists.
 * Supports both offset-based and cursor-based pagination.
 */
data class PaginationConfig(
    val pageSize: Int = 20,
    val prefetchDistance: Int = 5
)

/**
 * Pagination state for tracking load status.
 */
sealed class PaginationState<out T> {
    object Idle : PaginationState<Nothing>()
    object Loading : PaginationState<Nothing>()
    data class Success<T>(
        val items: List<T>,
        val hasMore: Boolean,
        val nextOffset: Int? = null,
        val nextCursor: String? = null
    ) : PaginationState<T>()
    data class Error(val message: String, val cause: Throwable? = null) : PaginationState<Nothing>()
}

/**
 * Pagination request parameters.
 */
data class PaginationRequest(
    val limit: Int,
    val offset: Int? = null,
    val cursor: String? = null
)

/**
 * Pagination response with metadata.
 */
data class PaginationResponse<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val totalCount: Int? = null,
    val nextOffset: Int? = null,
    val nextCursor: String? = null
)
