package `in`.xroden.flockr.features.shopping.domain

import `in`.xroden.flockr.features.shopping.model.ShoppingItem

sealed interface ShoppingUiState {
    data object Loading : ShoppingUiState
    data class Success(val items: List<ShoppingItem>) : ShoppingUiState
    data class Error(val message: String, val cause: Throwable? = null) : ShoppingUiState
}

sealed interface AddShoppingItemUiState {
    data object Idle : AddShoppingItemUiState
    data object Loading : AddShoppingItemUiState
    data object Success : AddShoppingItemUiState
    data class Error(val message: String) : AddShoppingItemUiState
}


