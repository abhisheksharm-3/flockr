package `in`.xroden.flockr.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.xroden.flockr.features.settings.ui.EditProfileScreen
import `in`.xroden.flockr.features.settings.ui.NotificationPreferencesScreen
import `in`.xroden.flockr.features.settings.ui.SecuritySettingsScreen
import `in`.xroden.flockr.features.settings.ui.SettingsScreen

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable<SettingsRoute> {
        SettingsScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToProfile = { navController.navigate(EditProfileRoute) },
            onNavigateToNotificationPreferences = { navController.navigate(NotificationPreferencesRoute) },
            onLogout = {
                // The authViewModel.signOut() in SettingsScreen will change auth state
                // which triggers the UI to show the unauthenticated (login) screen
                navController.popBackStack<HomeRoute>(inclusive = true)
            },
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
