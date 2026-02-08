package `in`.xroden.flockr.features.shopping.presentation

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.features.shopping.model.ShoppingItem

@Immutable
sealed interface ShoppingUiState {
    data object Loading : ShoppingUiState
    data class Success(val items: List<ShoppingItem>) : ShoppingUiState
    data class Error(val message: String, val cause: Throwable? = null) : ShoppingUiState
}

@Immutable
sealed interface AddShoppingItemUiState {
    data object Idle : AddShoppingItemUiState
    data object Loading : AddShoppingItemUiState
    data object Success : AddShoppingItemUiState
    data class Error(val message: String) : AddShoppingItemUiState
}


