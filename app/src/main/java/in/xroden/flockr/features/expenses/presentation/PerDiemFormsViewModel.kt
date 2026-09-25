/** The two per-diem forms: an item and its price, and a logged use of an item. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.expenses.data.PerDiemRepository
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemCategories
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.DEFAULT_CURRENCY_CODE
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.utils.lineTotal
import `in`.xroden.flockr.utils.parseDecimal
import `in`.xroden.flockr.utils.parseMoney
import `in`.xroden.flockr.utils.toAmountInput
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class PerDiemItemFormState(
    val configId: String? = null,
    val itemName: String = "",
    val rate: String = "",
    val unit: String = "unit",
    val category: String = PerDiemCategories.DEFAULT.first(),
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val isAdmin: Boolean = false,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = configId != null
    val parsedRate: BigDecimal? get() = parseMoney(rate, currencyCode)?.takeIf { it.signum() > 0 }
    val canSave: Boolean get() = isLoaded && !isSaving && itemName.isNotBlank() && unit.isNotBlank() && parsedRate != null
}

@HiltViewModel
class PerDiemItemFormViewModel @Inject constructor(
    private val perDiemRepository: PerDiemRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(PerDiemItemFormState())
    val form: StateFlow<PerDiemItemFormState> = _form.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)

    /** Emits once the item was saved, archived or deleted, which is when the form closes. */
    val done = _done.receiveAsFlow()

    fun initialize(houseId: String, configId: String?) {
        if (_form.value.isLoaded) return
        viewModelScope.launch {
            val currency = async { houseRepository.getHouseConfig(houseId).getOrNull().currency() }
            val viewerId = houseRepository.getCurrentUserId()
            val role = houseRepository.getHouseMembers(houseId).getOrNull()?.firstOrNull { it.userId == viewerId }?.role
            val base = PerDiemItemFormState(
                currencyCode = currency.await(),
                isAdmin = role == HouseMemberRole.OWNER || role == HouseMemberRole.ADMIN,
                isLoaded = configId == null,
            )
            val item = configId?.let { id -> perDiemRepository.getPerDiemConfigs(houseId).getOrNull()?.firstOrNull { it.id == id } }
            _form.value = when {
                configId == null -> base
                item == null -> base.copy(error = "This item no longer exists")
                else -> base.copy(
                    configId = item.id,
                    itemName = item.itemName,
                    rate = item.rate.toAmountInput(base.currencyCode),
                    unit = item.unit,
                    category = item.category,
                    isLoaded = true,
                )
            }
        }
    }

    fun update(transform: (PerDiemItemFormState) -> PerDiemItemFormState) = _form.update(transform)
    fun dismissError() = _form.update { it.copy(error = null) }

    fun save(houseId: String) {
        val form = _form.value
        val rate = form.parsedRate ?: return
        if (!form.canSave) return
        submit {
            if (form.configId == null) {
                perDiemRepository.createPerDiemConfig(houseId, form.itemName, rate, form.category, form.unit).map { }
            } else {
                perDiemRepository.updatePerDiemConfig(form.configId, form.itemName, rate, form.category, form.unit)
            }
        }
    }

    /** Stops the item taking new usage; past months still bill it. */
    fun archive() {
        val configId = _form.value.configId ?: return
        submit { perDiemRepository.deletePerDiemConfig(configId, deleteUsage = false) }
    }

    fun deleteWithUsage() {
        val configId = _form.value.configId ?: return
        submit { perDiemRepository.deletePerDiemConfig(configId, deleteUsage = true) }
    }

    private fun submit(action: suspend () -> Result<Unit>) {
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            action().fold(
                onSuccess = { _done.send(Unit) },
                onFailure = { error -> _form.update { it.copy(isSaving = false, error = error.userMessage()) } },
            )
        }
    }
}

data class PerDiemEntryFormState(
    val items: List<PerDiemConfig> = emptyList(),
    val configId: String? = null,
    val quantity: String = "1",
    val date: LocalDate? = null,
    val notes: String = "",
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val item: PerDiemConfig? get() = items.firstOrNull { it.id == configId }
    val parsedQuantity: BigDecimal? get() = parseDecimal(quantity)?.takeIf { it.signum() > 0 && it.stripTrailingZeros().scale() <= 3 }

    /** What this use will cost at the item's current price, rounded as the database will round it. */
    val cost: BigDecimal? get() = item?.let { item -> parsedQuantity?.let { lineTotal(it, item.rate, currencyCode) } }

    val canSave: Boolean get() = isLoaded && !isSaving && item != null && parsedQuantity != null && date != null
}

@HiltViewModel
class PerDiemEntryFormViewModel @Inject constructor(
    private val perDiemRepository: PerDiemRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(PerDiemEntryFormState())
    val form: StateFlow<PerDiemEntryFormState> = _form.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    /** Starts on [configId] when given, otherwise on the house's first item. */
    fun initialize(houseId: String, configId: String?) {
        if (_form.value.isLoaded) return
        viewModelScope.launch {
            val config = async { houseRepository.getHouseConfig(houseId).getOrNull() }
            perDiemRepository.getPerDiemConfigs(houseId).fold(
                onSuccess = { items ->
                    _form.value = PerDiemEntryFormState(
                        items = items,
                        configId = configId?.takeIf { id -> items.any { it.id == id } } ?: items.firstOrNull()?.id,
                        date = config.await().today(),
                        currencyCode = config.await().currency(),
                        isLoaded = true,
                    )
                },
                onFailure = { error -> _form.update { it.copy(error = error.userMessage()) } },
            )
        }
    }

    fun update(transform: (PerDiemEntryFormState) -> PerDiemEntryFormState) = _form.update(transform)
    fun dismissError() = _form.update { it.copy(error = null) }

    fun save() {
        val form = _form.value
        if (!form.canSave) return
        val configId = form.configId ?: return
        val quantity = form.parsedQuantity ?: return
        val date = form.date ?: return
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            perDiemRepository.addPerDiemEntry(configId, quantity, date, form.notes.ifBlank { null }).fold(
                onSuccess = { _saved.send(Unit) },
                onFailure = { error -> _form.update { it.copy(isSaving = false, error = error.userMessage()) } },
            )
        }
    }
}
