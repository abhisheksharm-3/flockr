package `in`.xroden.flockr.core.ui.pagination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for handling paginated data loading.
 * Provides automatic pagination logic with load more capability.
 *
 * @param T The type of items being paginated
 */
abstract class PaginatedViewModel<T> : ViewModel() {

    private val _paginationState = MutableStateFlow<PaginationState<T>>(PaginationState.Idle)
    val paginationState: StateFlow<PaginationState<T>> = _paginationState.asStateFlow()

    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    private var currentOffset = 0
    private var currentCursor: String? = null
    private var hasMore = true
    private var isLoading = false

    protected open val config = PaginationConfig()

    /**
     * Abstract function to fetch a page of items.
     * Implement this in your specific ViewModel.
     */
    protected abstract suspend fun fetchPage(request: PaginationRequest): Result<PaginationResponse<T>>

    /**
     * Load the first page of items.
     */
    fun loadFirstPage() {
        viewModelScope.launch {
            _paginationState.value = PaginationState.Loading
            _items.value = emptyList()
            currentOffset = 0
            currentCursor = null
            hasMore = true
            isLoading = true

            val request = PaginationRequest(
                limit = config.pageSize,
                offset = 0,
                cursor = null
            )

            fetchPage(request).fold(
                onSuccess = { response ->
                    _items.value = response.items
                    currentOffset = response.items.size
                    currentCursor = response.nextCursor
                    hasMore = response.hasMore
                    _paginationState.value = PaginationState.Success(
                        items = response.items,
                        hasMore = response.hasMore,
                        nextOffset = response.nextOffset,
                        nextCursor = response.nextCursor
                    )
                    isLoading = false
                },
                onFailure = { error ->
                    _paginationState.value = PaginationState.Error(
                        message = error.message ?: "Failed to load items",
                        cause = error
                    )
                    isLoading = false
                }
            )
        }
    }

    /**
     * Load the next page of items (for infinite scroll).
     */
    fun loadNextPage() {
        if (isLoading || !hasMore) return

        viewModelScope.launch {
            isLoading = true

            val request = PaginationRequest(
                limit = config.pageSize,
                offset = currentOffset,
                cursor = currentCursor
            )

            fetchPage(request).fold(
                onSuccess = { response ->
                    _items.value = _items.value + response.items
                    currentOffset += response.items.size
                    currentCursor = response.nextCursor
                    hasMore = response.hasMore
                    _paginationState.value = PaginationState.Success(
                        items = _items.value,
                        hasMore = response.hasMore,
                        nextOffset = response.nextOffset,
                        nextCursor = response.nextCursor
                    )
                    isLoading = false
                },
                onFailure = { error ->
                    _paginationState.value = PaginationState.Error(
                        message = error.message ?: "Failed to load more items",
                        cause = error
                    )
                    isLoading = false
                }
            )
        }
    }

    /**
     * Refresh the current page.
     */
    fun refresh() {
        loadFirstPage()
    }
}
