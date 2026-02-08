package `in`.xroden.flockr.ui.navigation.state

/**
 * Authentication navigation states for controlling navigation flow.
 */
sealed class AuthNavigationState {
    object Loading : AuthNavigationState()
    object Unauthenticated : AuthNavigationState()
    object NeedsOnboarding : AuthNavigationState()
    object Authenticated : AuthNavigationState()
}
