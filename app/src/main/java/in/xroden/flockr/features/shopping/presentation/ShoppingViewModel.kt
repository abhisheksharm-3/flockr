package `in`.xroden.flockr.features.shopping.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.shopping.data.IShoppingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: IShoppingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingUiState>(ShoppingUiState.Loading)
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private val _addItemState = MutableStateFlow<AddShoppingItemUiState>(AddShoppingItemUiState.Idle)
    val addItemState: StateFlow<AddShoppingItemUiState> = _addItemState.asStateFlow()

    private val _events = Channel<ShoppingEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var shoppingJob: Job? = null
    private var currentHouseId: String? = null

    fun loadShoppingItems(houseId: String) {
        if (currentHouseId == houseId && shoppingJob?.isActive == true) return

        shoppingJob?.cancel()
        currentHouseId = houseId

        shoppingJob = viewModelScope.launch {
            if (_uiState.value !is ShoppingUiState.Success) {
                _uiState.value = ShoppingUiState.Loading
            }

            shoppingRepository.getShoppingItemsFlow(houseId).collect { result ->
                result.fold(
                    onSuccess = { items -> _uiState.value = ShoppingUiState.Success(items) },
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

    fun addItem(houseId: String, itemName: String, quantity: String?) {
        viewModelScope.launch {
            _addItemState.value = AddShoppingItemUiState.Loading

            shoppingRepository.addShoppingItem(houseId, itemName, quantity).fold(
                onSuccess = {
                    _addItemState.value = AddShoppingItemUiState.Success
                    _events.send(ShoppingEvent.ItemAdded)
                    loadShoppingItems(houseId)
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
            val currentState = _uiState.value
            if (currentState is ShoppingUiState.Success) {
                _uiState.value = ShoppingUiState.Success(
                    currentState.items.map { if (it.id == itemId) it.copy(isPurchased = true) else it }
                )
            }

            shoppingRepository.markAsPurchased(itemId, houseId, itemName).onFailure { error ->
                loadShoppingItems(houseId)
                _events.send(ShoppingEvent.Error(error.message ?: "Failed to mark as purchased"))
            }
        }
    }

    fun updateItem(itemId: String, itemName: String?, quantity: String?) {
        viewModelScope.launch {
            shoppingRepository.updateShoppingItem(itemId, itemName, quantity).onFailure { error ->
                _uiState.value = ShoppingUiState.Error(
                    message = error.message ?: "Failed to update item",
                    cause = error
                )
            }
        }
    }

    fun deleteItem(itemId: String, houseId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ShoppingUiState.Success) {
                _uiState.value = ShoppingUiState.Success(
                    currentState.items.filter { it.id != itemId }
                )
            }

            shoppingRepository.deleteShoppingItem(itemId, houseId).onFailure { error ->
                loadShoppingItems(houseId)
                _events.send(ShoppingEvent.Error(error.message ?: "Failed to delete item"))
            }
        }
    }

    fun clearPurchasedItems(houseId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ShoppingUiState.Success) {
                _uiState.value = ShoppingUiState.Success(
                    currentState.items.filter { !it.isPurchased }
                )
            }

            shoppingRepository.clearPurchasedItems(houseId).onFailure { error ->
                loadShoppingItems(houseId)
                _events.send(ShoppingEvent.Error(error.message ?: "Failed to clear items"))
            }
        }
    }

    fun resetAddItemState() {
        _addItemState.value = AddShoppingItemUiState.Idle
    }
}

sealed class ShoppingEvent {
    data object ItemAdded : ShoppingEvent()
    data class Error(val message: String) : ShoppingEvent()
}
