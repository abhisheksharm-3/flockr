/** What this phone's own sharing is doing, so the screen can show "starting" and explain a failure. */
package `in`.xroden.flockr.features.location.system

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The phone's side of sharing. Whether others can see the viewer is the server's to say, through
 * the house's live locations; this carries only what the server can't: that a start is under way,
 * or why it failed.
 */
@Singleton
class LocationSharingStatus @Inject constructor() {

    sealed interface State {
        data object Idle : State
        data class Starting(val houseId: String) : State
        data class Sharing(val houseId: String, val expiresAt: Instant) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun starting(houseId: String) { _state.value = State.Starting(houseId) }
    fun sharing(houseId: String, expiresAt: Instant) { _state.value = State.Sharing(houseId, expiresAt) }
    fun failed(message: String) { _state.value = State.Failed(message) }
    fun stopped() { if (_state.value !is State.Failed) _state.value = State.Idle }

    /** Clears a failure once the screen has shown it. */
    fun acknowledge() { if (_state.value is State.Failed) _state.value = State.Idle }
}
