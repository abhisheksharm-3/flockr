package `in`.xroden.flockr.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.xroden.flockr.features.settings.ui.EditProfileScreen
import `in`.xroden.flockr.features.notifications.ui.NotificationPreferencesScreen
import `in`.xroden.flockr.features.settings.ui.SecuritySettingsScreen
import `in`.xroden.flockr.features.settings.ui.SettingsScreen

/**
 * The settings screens. Signing out and deleting the account run on the app-wide [authViewModel]
 * rather than one scoped to the settings screen: both remove the screens they run from, which would
 * cancel work started in their own scope halfway, leaving the member signed in on a blank screen.
 */
fun NavGraphBuilder.settingsGraph(navController: NavController, authViewModel: AuthViewModel) {
    composable<SettingsRoute> {
        val accountDeletion by authViewModel.accountDeletion.collectAsStateWithLifecycle()
        SettingsScreen(
            accountDeletion = accountDeletion,
            onDeleteAccount = authViewModel::deleteAccount,
            onAccountDeletionErrorShown = authViewModel::dismissAccountDeletionError,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToProfile = { navController.navigate(EditProfileRoute) },
            onNavigateToNotificationPreferences = { navController.navigate(NotificationPreferencesRoute) },
            onSignOut = authViewModel::signOut,
            onNavigateToSecurity = {
                navController.navigate(SecuritySettingsRoute)
            }
        )
    }

    composable<SecuritySettingsRoute> {
        SecuritySettingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<NotificationPreferencesRoute> {
        NotificationPreferencesScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<EditProfileRoute> {
        EditProfileScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
