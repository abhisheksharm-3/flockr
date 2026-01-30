package `in`.xroden.flockr.core.ui.pagination

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Composable for handling infinite scroll/lazy loading in LazyColumn.
 * Automatically triggers load more when user scrolls near the end.
 *
 * @param listState The LazyListState from the LazyColumn
 * @param buffer Number of items before end to trigger load (default: 5)
 * @param onLoadMore Callback to load more items
 */
@Composable
fun InfiniteScrollHandler(
    listState: LazyListState,
    buffer: Int = 5,
    onLoadMore: () -> Unit
) {
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleItemIndex >= (totalItemsCount - buffer)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad) {
                    onLoadMore()
                }
            }
    }
}

/**
 * Extension function to check if LazyListState has reached the end.
 */
@Composable
fun LazyListState.isScrolledToEnd(buffer: Int = 5): Boolean {
    return remember(this) {
        derivedStateOf {
            val layoutInfo = layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            totalItemsCount > 0 && lastVisibleItemIndex >= (totalItemsCount - buffer)
        }
    }.value
}

/**
 * Extension function to check if LazyListState is at the top.
 */
@Composable
fun LazyListState.isScrolledToTop(): Boolean {
    return remember(this) {
        derivedStateOf {
            firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
        }
    }.value
}
