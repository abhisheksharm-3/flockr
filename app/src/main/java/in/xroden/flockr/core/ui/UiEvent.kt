package `in`.xroden.flockr.core.ui

/**
 * Base sealed class for one-shot UI events that should be consumed only once.
 * Use with Channel<UiEvent> in ViewModels instead of delay() + state reset patterns.
 */
sealed class UiEvent {
    /**
     * Navigation event to navigate back
     */
    object NavigateBack : UiEvent()

    /**
     * Show a snackbar message
     */
    data class ShowSnackbar(val message: String) : UiEvent()

    /**
     * Show a toast message
     */
    data class ShowToast(val message: String) : UiEvent()

    /**
     * Success event with optional message
     */
    data class Success(val message: String? = null) : UiEvent()

    /**
     * Error event with message
     */
    data class Error(val message: String) : UiEvent()
}
