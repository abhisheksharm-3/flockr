package `in`.xroden.flockr.ui.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.auth.ui.NewPasswordScreen
import `in`.xroden.flockr.features.auth.presentation.PasswordResetState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.features.house.ui.home.HomeScreen
import `in`.xroden.flockr.features.notifications.presentation.NotificationViewModel
import `in`.xroden.flockr.features.notifications.ui.NotificationScreen
import `in`.xroden.flockr.ui.components.loading.FlockrSplashLoader
import `in`.xroden.flockr.features.auth.presentation.AuthNavigationState
import androidx.compose.runtime.key

/**
 * The app's navigation, switching between the signed-out and signed-in graphs. [initialInviteCode]
 * and [pendingNotificationId] are deep links held until the user is signed in; each consumed
 * callback clears one once it has been followed. [onSignedIn] runs each time the user reaches the app
 * signed in, which is when asking for the notification permission makes sense.
 *
 * A notification or invite that arrives signed out waits until sign-in, then opens. The signed-in
 * graph stays up through a brief Loading after the first sign-in, so a token refresh doesn't flash
 * the splash.
 */
@Composable
fun FlockrNavigation(
    initialInviteCode: String? = null,
    onInviteConsumed: () -> Unit = {},
    pendingNotificationId: String? = null,
    onNotificationConsumed: () -> Unit = {},
    onSignedIn: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val authUiState by authViewModel.authNavigationState.collectAsState(initial = AuthNavigationState.Loading)
    val hasAuthenticatedSession = remember { mutableStateOf(false) }

    LaunchedEffect(authUiState) {
        if (authUiState is AuthNavigationState.Authenticated) {
            hasAuthenticatedSession.value = true
        } else if (authUiState is AuthNavigationState.Unauthenticated || authUiState is AuthNavigationState.NeedsOnboarding) {
            hasAuthenticatedSession.value = false
        }
    }

    LaunchedEffect(authUiState is AuthNavigationState.Authenticated) {
        if (authUiState is AuthNavigationState.Authenticated) onSignedIn()
    }
    LaunchedEffect(authUiState, pendingNotificationId) {
        val id = pendingNotificationId ?: return@LaunchedEffect
        if (authUiState !is AuthNavigationState.Authenticated) return@LaunchedEffect
        notificationViewModel.openById(id)?.let { navController.navigate(it.destination()) }
        onNotificationConsumed()
    }
    LaunchedEffect(authUiState, initialInviteCode) {
        val code = initialInviteCode
        if (code != null && authUiState is AuthNavigationState.Authenticated) {
            navController.navigate(JoinHousePreviewRoute(code))
            onInviteConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (authUiState is AuthNavigationState.Authenticated || (hasAuthenticatedSession.value && authUiState is AuthNavigationState.Loading)) {
            key("authenticated") {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    enterTransition = SharedAxisEnter,
                    exitTransition = SharedAxisExit,
                    popEnterTransition = SharedAxisPopEnter,
                    popExitTransition = SharedAxisPopExit,
                ) {
                    composable<HomeRoute> {
                        HomeScreen(
                            onHouseClick = { houseId ->
                                navController.navigate(HouseDetailsRoute(houseId))
                            },
                            onNotificationsClick = {
                                navController.navigate(NotificationsRoute)
                            },
                            onSettingsClick = {
                                navController.navigate(SettingsRoute)
                            },
                            onCreateHouseClick = {
                                navController.navigate(CreateHouseRoute)
                            },
                            onJoinHouseClick = {
                                navController.navigate(JoinHouseRoute)
                            },
                            onAddExpense = { houseId -> navController.navigate(ExpenseFormRoute(houseId)) },
                            onSettleUp = { houseId, payment ->
                                navController.navigate(SettleUpRoute(houseId, payment.fromUserId, payment.toUserId, payment.amount.toPlainString()))
                            },
                            onOpenBills = { houseId -> navController.navigate(BillsRoute(houseId)) },
                            onOpenChores = { houseId -> navController.navigate(ChoresRoute(houseId)) },
                        )
                    }

                    composable<NotificationsRoute> {
                        NotificationScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onOpen = { navController.navigate(it.destination()) },
                        )
                    }

                    houseGraph(navController)
                    expenseGraph(navController)
                    settingsGraph(navController, authViewModel)
                }
            }
        }

        if (authUiState is AuthNavigationState.Unauthenticated) {
            key("unauthenticated") {
                NavHost(
                    navController = navController,
                    startDestination = WelcomeRoute,
                    enterTransition = SharedAxisEnter,
                    exitTransition = SharedAxisExit,
                    popEnterTransition = SharedAxisPopEnter,
                    popExitTransition = SharedAxisPopExit,
                ) {
                    authGraph(navController)
                }
            }
        }

        if (authUiState is AuthNavigationState.NeedsOnboarding) {
            key("onboarding") {
                NavHost(
                    navController = navController,
                    startDestination = OnboardingRoute,
                    enterTransition = SharedAxisEnter,
                    exitTransition = SharedAxisExit,
                    popEnterTransition = SharedAxisPopEnter,
                    popExitTransition = SharedAxisPopExit,
                ) {
                    onboardingGraph(navController)
                }
            }
        }

        val passwordReset by authViewModel.passwordReset.collectAsStateWithLifecycle()
        if (authUiState is AuthNavigationState.Authenticated &&
            (passwordReset is PasswordResetState.ChoosingPassword || passwordReset == PasswordResetState.Saving)
        ) {
            NewPasswordScreen(passwordReset, onSave = authViewModel::setNewPassword)
        }

        if (authUiState is AuthNavigationState.Loading && !hasAuthenticatedSession.value) {
            FlockrSplashLoader()
        }
    }
}
