package `in`.xroden.flockr.features.shopping.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.shopping.data.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingUiState>(ShoppingUiState.Loading)
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private val _addItemState = MutableStateFlow<AddShoppingItemUiState>(AddShoppingItemUiState.Idle)
    val addItemState: StateFlow<AddShoppingItemUiState> = _addItemState.asStateFlow()

    fun loadShoppingItems(houseId: String) {
        viewModelScope.launch {
            _uiState.value = ShoppingUiState.Loading
            
            shoppingRepository.getShoppingItemsFlow(houseId).collect { result ->
                result.fold(
                    onSuccess = { items ->
                        _uiState.value = ShoppingUiState.Success(items)
                    },
                    onFailure = { error ->
                        _uiState.value = ShoppingUiState.Error(
                            message = error.message ?: "Failed to load shopping items",
                            cause = error
                        )
                    }
                )
            }
        }
    }

    fun addItem(
        houseId: String,
        itemName: String,
        quantity: String?
    ) {
        viewModelScope.launch {
            _addItemState.value = AddShoppingItemUiState.Loading
            
            shoppingRepository.addShoppingItem(houseId, itemName, quantity).fold(
                onSuccess = {
                    _addItemState.value = AddShoppingItemUiState.Success
                    kotlinx.coroutines.delay(1000)
                    _addItemState.value = AddShoppingItemUiState.Idle
                },
                onFailure = { error ->
                    _addItemState.value = AddShoppingItemUiState.Error(
                        message = error.message ?: "Failed to add item"
                    )
                }
            )
        }
    }

    fun markAsPurchased(itemId: String, houseId: String, itemName: String) {
        viewModelScope.launch {
            shoppingRepository.markAsPurchased(itemId, houseId, itemName).fold(
                onSuccess = {
                    // Success - state updated via flow
                },
                onFailure = { error ->
                    _uiState.value = ShoppingUiState.Error(
                        message = error.message ?: "Failed to mark item as purchased",
                        cause = error
                    )
                }
            )
        }
    }

    fun updateItem(
        itemId: String,
        itemName: String?,
        quantity: String?
    ) {
        viewModelScope.launch {
            shoppingRepository.updateShoppingItem(itemId, itemName, quantity).fold(
                onSuccess = {
                    // Success - state updated via flow
                },
                onFailure = { error ->
                    _uiState.value = ShoppingUiState.Error(
                        message = error.message ?: "Failed to update item",
                        cause = error
                    )
                }
            )
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingItem(itemId).fold(
                onSuccess = {
                    // Success - state updated via flow
                },
                onFailure = { error ->
                    _uiState.value = ShoppingUiState.Error(
                        message = error.message ?: "Failed to delete item",
                        cause = error
                    )
                }
            )
        }
    }

    fun resetAddItemState() {
        _addItemState.value = AddShoppingItemUiState.Idle
    }
}
