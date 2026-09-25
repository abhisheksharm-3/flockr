/** A house's chores: the open ones, the done ones, and who has done the most this month. */
package `in`.xroden.flockr.features.chores.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.features.chores.data.ChoreRepository
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.timeZone
import `in`.xroden.flockr.features.house.model.today
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/** A member's standing on the leaderboard: effort points and chores done this calendar month. */
data class ChoreScore(val userId: String, val points: Int, val done: Int)

sealed interface ChoresUiState {
    data object Loading : ChoresUiState
    data class Error(val message: String) : ChoresUiState

    /** [today] is the date where the house is, which "due" and "overdue" are measured against. */
    data class Ready(
        val open: List<Chore>,
        val done: List<Chore>,
        val scores: List<ChoreScore>,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
        val today: LocalDate,
    ) : ChoresUiState
}

@HiltViewModel
class ChoresViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ChoresUiState>(ChoresUiState.Loading)
    val state: StateFlow<ChoresUiState> = _state.asStateFlow()

    private val _notices = Channel<Notice>(Channel.BUFFERED)
    val notices = _notices.receiveAsFlow()

    private var loadJob: Job? = null

    fun load(houseId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }.associateBy { it.userId } }
            val config = async { houseRepository.getHouseConfig(houseId).getOrNull() }
            val viewerId = choreRepository.getCurrentUserId().orEmpty()
            choreRepository.getChoresFlow(houseId).collect { result ->
                _state.value = result.fold(
                    onSuccess = { chores ->
                        val (done, open) = chores.partition { it.isCompleted }
                        ChoresUiState.Ready(
                            open = open,
                            done = done.sortedByDescending { it.completedAt },
                            scores = scoresThisMonth(done, config.await()),
                            members = members.await(),
                            viewerId = viewerId,
                            today = config.await().today(),
                        )
                    },
                    onFailure = { ChoresUiState.Error(it.userMessage()) },
                )
            }
        }
    }

    fun setCompleted(chore: Chore, isCompleted: Boolean) {
        viewModelScope.launch {
            choreRepository.setCompleted(chore.id, isCompleted).onFailure { _notices.send(Notice(it.userMessage(), isError = true)) }
        }
    }

    fun delete(chore: Chore) {
        viewModelScope.launch {
            choreRepository.deleteChore(chore.id).onFailure { _notices.send(Notice(it.userMessage(), isError = true)) }
        }
    }

    fun clearDone(houseId: String) {
        viewModelScope.launch {
            choreRepository.clearCompletedChores(houseId).onFailure { _notices.send(Notice(it.userMessage(), isError = true)) }
        }
    }

    /** Points for chores completed in the current month where the house is, highest first. */
    private fun scoresThisMonth(done: List<Chore>, config: HouseConfig?): List<ChoreScore> {
        val today = config.today()
        val zone = config.timeZone()
        return done
            .filter { chore ->
                val completedOn = chore.completedAt?.toLocalDateTime(zone)?.date ?: return@filter false
                completedOn.year == today.year && completedOn.month == today.month
            }
            .groupBy { it.completedBy }
            .mapNotNull { (userId, chores) -> userId?.let { ChoreScore(it, chores.sumOf { chore -> chore.effortPoints }, chores.size) } }
            .sortedByDescending { it.points }
    }
}
