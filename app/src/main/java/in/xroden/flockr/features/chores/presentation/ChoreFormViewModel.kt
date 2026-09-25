/** Adding or editing a chore: what it is, when, how often, whose turn, and how much it counts. */
package `in`.xroden.flockr.features.chores.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.data.enums.ChoreRecurrence
import `in`.xroden.flockr.features.chores.data.ChoreDraft
import `in`.xroden.flockr.features.chores.data.ChoreRepository
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.today
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

/** [rotation] is in turn order: the order members were ticked in. */
data class ChoreFormState(
    val choreId: String? = null,
    val taskName: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val recurrence: ChoreRecurrence? = null,
    val assignedTo: String? = null,
    val rotation: List<String> = emptyList(),
    val effortPoints: Int = 1,
    val members: List<MemberWithProfile> = emptyList(),
    val today: LocalDate? = null,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val isEditing: Boolean get() = choreId != null
    val canSave: Boolean get() = isLoaded && !isSaving && taskName.isNotBlank()

    /** Whoever is first in the rotation takes the chore when no one is picked. */
    fun toDraft() = ChoreDraft(
        taskName = taskName,
        description = description,
        dueDate = dueDate,
        recurrencePattern = recurrence,
        rotation = rotation,
        effortPoints = effortPoints,
        assignedTo = assignedTo ?: rotation.firstOrNull().takeIf { recurrence != null },
    )
}

@HiltViewModel
class ChoreFormViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(ChoreFormState())
    val form: StateFlow<ChoreFormState> = _form.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    fun initialize(houseId: String, choreId: String?) {
        if (_form.value.isLoaded) return
        viewModelScope.launch {
            val today = async { houseRepository.getHouseConfig(houseId).getOrNull().today() }
            val members = houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }.filter { it.isActive }
            val chore = choreId?.let { choreRepository.getChore(it).getOrNull() }
            _form.value = if (chore == null) {
                ChoreFormState(members = members, today = today.await(), dueDate = today.await(), assignedTo = choreRepository.getCurrentUserId(), isLoaded = true)
            } else {
                editing(chore, members, today.await())
            }
        }
    }

    fun update(transform: (ChoreFormState) -> ChoreFormState) = _form.update(transform)
    fun dismissError() = _form.update { it.copy(error = null) }

    fun toggleRotation(userId: String) = _form.update {
        it.copy(rotation = if (userId in it.rotation) it.rotation - userId else it.rotation + userId)
    }

    fun save(houseId: String) {
        val form = _form.value
        if (!form.canSave) return
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = if (form.choreId == null) choreRepository.createChore(houseId, form.toDraft()) else choreRepository.updateChore(form.choreId, form.toDraft())
            result.fold(
                onSuccess = { _saved.send(Unit) },
                onFailure = { error -> _form.update { it.copy(isSaving = false, error = error.userMessage()) } },
            )
        }
    }

    private fun editing(chore: Chore, members: List<MemberWithProfile>, today: LocalDate) = ChoreFormState(
        choreId = chore.id,
        taskName = chore.taskName,
        description = chore.description.orEmpty(),
        dueDate = chore.dueDate,
        recurrence = chore.recurrencePattern,
        assignedTo = chore.assignedTo,
        rotation = chore.rotation,
        effortPoints = chore.effortPoints,
        members = members,
        today = today,
        isLoaded = true,
    )
}
