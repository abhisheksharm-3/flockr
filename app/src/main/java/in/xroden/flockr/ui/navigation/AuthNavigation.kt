package `in`.xroden.flockr.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import `in`.xroden.flockr.features.auth.ui.LoginScreen
import `in`.xroden.flockr.features.auth.ui.OnboardingScreen
import `in`.xroden.flockr.features.auth.ui.SignupScreen
import `in`.xroden.flockr.features.auth.ui.WelcomeScreen

fun NavGraphBuilder.authGraph(navController: NavController) {
    composable<WelcomeRoute> {
        WelcomeScreen(
            onGetStarted = {
                navController.navigate(SignupRoute)
            },
            onSignIn = {
                navController.navigate(LoginRoute)
            }
        )
    }

    composable<LoginRoute> {
        LoginScreen(
            onNavigateToSignup = { navController.navigate(SignupRoute) },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<SignupRoute> {
        SignupScreen(
            onNavigateToLogin = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.onboardingGraph(navController: NavController) {
    composable<OnboardingRoute> {
        OnboardingScreen(
            onComplete = {
                // Navigation will be handled by auth state change
            }
        )
    }
}
