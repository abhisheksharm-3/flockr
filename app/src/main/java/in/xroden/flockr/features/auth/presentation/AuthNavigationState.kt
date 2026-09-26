package `in`.xroden.flockr.features.auth.presentation

/**
 * Authentication navigation states for controlling navigation flow.
 */
sealed class AuthNavigationState {
    object Loading : AuthNavigationState()
    object Unauthenticated : AuthNavigationState()
    object NeedsOnboarding : AuthNavigationState()
    object Authenticated : AuthNavigationState()
}
