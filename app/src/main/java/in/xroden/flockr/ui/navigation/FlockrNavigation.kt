package `in`.xroden.flockr.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import `in`.xroden.flockr.data.enums.NotificationType
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.features.house.ui.home.HomeScreen
import `in`.xroden.flockr.features.notifications.ui.NotificationScreen
import `in`.xroden.flockr.ui.components.loading.FlockrSplashLoader
import `in`.xroden.flockr.ui.navigation.state.AuthNavigationState
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject

/**
 * Navigation component for Flockr app with authentication state management.
 * Uses modular navigation graphs for different features with type-safe routes.
 */

@Composable
fun FlockrNavigation(
    initialInviteCode: String? = null,
    onInviteConsumed: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val authUiState by authViewModel.authNavigationState.collectAsState(initial = AuthNavigationState.Loading)

    // State to track if we have successfully loaded the authenticated graph at least once
    val hasAuthenticatedSession = remember { mutableStateOf(false) }

    LaunchedEffect(authUiState) {
        if (authUiState is AuthNavigationState.Authenticated) {
            hasAuthenticatedSession.value = true
        } else if (authUiState is AuthNavigationState.Unauthenticated || authUiState is AuthNavigationState.NeedsOnboarding) {
            hasAuthenticatedSession.value = false
        }
    }

    // Invite deep link: once authenticated, jump straight into the join preview with the code.
    // If the link arrives while signed out, this waits until auth completes.
    LaunchedEffect(authUiState, initialInviteCode) {
        val code = initialInviteCode
        if (code != null && authUiState is AuthNavigationState.Authenticated) {
            navController.navigateToJoinHousePreview(code)
            onInviteConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Authenticated Content (Main App)
        if (authUiState is AuthNavigationState.Authenticated || (hasAuthenticatedSession.value && authUiState is AuthNavigationState.Loading)) {
            key("authenticated") {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    // Core navigation: Home and Notifications (kept inline due to complex deep-link handling)
                    composable<HomeRoute> {
                        HomeScreen(
                            onHouseClick = { houseId ->
                                navController.navigateToHouseDetails(houseId)
                            },
                            onNotificationsClick = {
                                navController.navigateToNotifications()
                            },
                            onSettingsClick = {
                                navController.navigateToSettings()
                            },
                            onCreateHouseClick = {
                                navController.navigateToCreateHouse()
                            },
                            onJoinHouseClick = {
                                navController.navigateToJoinHouse()
                            },
                            onNavigateToJoinPreview = { inviteCode ->
                                navController.navigateToJoinHousePreview(inviteCode)
                            }
                        )
                    }

                    composable<NotificationsRoute> {
                        val context = LocalContext.current
                        NotificationScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNotificationClick = { notification ->
                                val houseId = notification.houseId
                                if (houseId != null) {
                                    when (notification.type) {
                                        NotificationType.HOUSE_INVITE -> {
                                            val inviteCode = try {
                                                val data = notification.data
                                                if (!data.isNullOrEmpty()) {
                                                    val json = JSONObject(data)
                                                    json.optString("invite_code").takeIf { it.isNotEmpty() }
                                                        ?: json.optString("code").takeIf { it.isNotEmpty() }
                                                } else null
                                            } catch (_: Exception) {
                                                null
                                            }

                                            if (!inviteCode.isNullOrEmpty()) {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("flockr://invite/$inviteCode")
                                                )
                                                intent.setPackage(context.packageName)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            } else {
                                                navController.navigateToHome(clearBackStack = true)
                                            }
                                        }
                                        NotificationType.EXPENSE, NotificationType.EXPENSE_SPLIT,
                                        NotificationType.SETTLEMENT, NotificationType.PER_DIEM -> {
                                            navController.navigateToExpenseDashboard(houseId)
                                        }
                                        NotificationType.SHOPPING, NotificationType.SHOPPING_ITEM -> {
                                            navController.navigateToShoppingList(houseId)
                                        }
                                        NotificationType.CHORE, NotificationType.CHORE_ASSIGNED -> {
                                            navController.navigateToChores(houseId)
                                        }
                                        NotificationType.MESSAGE, NotificationType.MESSAGE_SENT -> {
                                            navController.navigateToChat(houseId)
                                        }
                                        NotificationType.DOCUMENT -> {
                                            navController.navigateToDocuments(houseId)
                                        }
                                        else -> {
                                            navController.navigateToHouseDetails(houseId)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Feature-specific navigation graphs
                    houseGraph(navController)
                    expenseGraph(navController)
                    settingsGraph(navController)
                }
            }
        }

        // Unauthenticated Content
        if (authUiState is AuthNavigationState.Unauthenticated) {
            key("unauthenticated") {
                NavHost(
                    navController = navController,
                    startDestination = WelcomeRoute
                ) {
                    authGraph(navController)
                }
            }
        }

        // Onboarding Content
        if (authUiState is AuthNavigationState.NeedsOnboarding) {
            key("onboarding") {
                NavHost(
                    navController = navController,
                    startDestination = OnboardingRoute
                ) {
                    onboardingGraph(navController)
                }
            }
        }

        // Loading Overlay
        if (authUiState is AuthNavigationState.Loading && !hasAuthenticatedSession.value) {
            FlockrSplashLoader()
        }
    }
}
