package `in`.xroden.flockr.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.xroden.flockr.features.settings.ui.EditProfileScreen
import `in`.xroden.flockr.features.notifications.ui.NotificationPreferencesScreen
import `in`.xroden.flockr.features.settings.ui.SecuritySettingsScreen
import `in`.xroden.flockr.features.settings.ui.SettingsScreen

/**
 * The settings screens. [onSignOut] comes from the app-wide auth view model rather than one scoped
 * to the settings screen: signing out removes the screens it runs from, which would cancel a
 * sign-out started in their own scope halfway, leaving the member signed in on a blank screen.
 */
fun NavGraphBuilder.settingsGraph(navController: NavController, onSignOut: () -> Unit) {
    composable<SettingsRoute> {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToProfile = { navController.navigate(EditProfileRoute) },
            onNavigateToNotificationPreferences = { navController.navigate(NotificationPreferencesRoute) },
            onSignOut = onSignOut,
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
