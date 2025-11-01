package `in`.xroden.flockr.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `in`.xroden.flockr.ui.screens.auth.LoginScreen
import `in`.xroden.flockr.ui.screens.auth.SignupScreen
import `in`.xroden.flockr.ui.screens.chat.ChatScreen
import `in`.xroden.flockr.ui.screens.chores.ChoresScreen
import `in`.xroden.flockr.ui.screens.documents.DocumentsScreen
import `in`.xroden.flockr.ui.screens.expenses.AddExpenseScreen
import `in`.xroden.flockr.ui.screens.expenses.AddPerDiemEntryScreen
import `in`.xroden.flockr.ui.screens.expenses.BalancesScreen
import `in`.xroden.flockr.ui.screens.expenses.ExpenseDashboardScreen
import `in`.xroden.flockr.ui.screens.expenses.ExpensesScreen
import `in`.xroden.flockr.ui.screens.home.CreateHouseScreen
import `in`.xroden.flockr.ui.screens.home.HomeScreen
import `in`.xroden.flockr.ui.screens.house.HouseDetailsScreen
import `in`.xroden.flockr.ui.screens.notifications.NotificationScreen
import `in`.xroden.flockr.ui.screens.onboarding.OnboardingScreen
import `in`.xroden.flockr.ui.screens.settings.SettingsScreen
import `in`.xroden.flockr.ui.screens.shopping.ShoppingListScreen
import `in`.xroden.flockr.ui.viewmodel.AuthViewModel
import io.github.jan.supabase.gotrue.SessionStatus

//
// 1. MOVED: AuthNavigationState sealed class must be at the top level,
//    not inside the composable function.
//
sealed class AuthNavigationState {
    object Loading : AuthNavigationState()
    object Unauthenticated : AuthNavigationState()
    object NeedsOnboarding : AuthNavigationState()
    object Authenticated : AuthNavigationState()
}

@Composable
fun FlockrNavigation(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    // These properties were marked as "never used" because the 'when' block
    // was broken. They are correct.
    val sessionStatus by authViewModel.sessionStatus.collectAsState(initial = SessionStatus.LoadingFromStorage)
    val profile by authViewModel.profile.collectAsState(initial = null)
    val authUiState by authViewModel.authNavigationState.collectAsState(initial = AuthNavigationState.Loading)

    // Log navigation state changes
    androidx.compose.runtime.LaunchedEffect(authUiState) {
        android.util.Log.d("FlockrNavigation", "🎯 UI showing: ${authUiState::class.simpleName}")
    }

    //
    // 2. FIXED: The 'when' block was syntactically incorrect.
    //    It is now properly structured with 'is' cases.
    //
    when (authUiState) {
        is AuthNavigationState.Loading -> {
            android.util.Log.d("FlockrNavigation", "📱 Rendering loading screen")
            // Show a loading indicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is AuthNavigationState.Unauthenticated -> {
            androidx.compose.runtime.key("unauthenticated") {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Login.route
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
                        )
                    }

                    composable(Screen.Signup.route) {
                        SignupScreen(
                            onNavigateToLogin = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
        is AuthNavigationState.NeedsOnboarding -> {
            androidx.compose.runtime.key("onboarding") {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Onboarding.route
                ) {
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            onComplete = {
                                // Navigation will be handled by recomposition
                            }
                        )
                    }
                }
            }
        }
        is AuthNavigationState.Authenticated -> {
            androidx.compose.runtime.key("authenticated") {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                // Main screens
                composable(Screen.Home.route) {
                    HomeScreen(
                        onHouseClick = { houseId ->
                            navController.navigate(Screen.HouseDetails.createRoute(houseId))
                        },
                        onNotificationsClick = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onCreateHouseClick = {
                            navController.navigate(Screen.CreateHouse.route)
                        },
                        onJoinHouseClick = {
                            navController.navigate(Screen.JoinHouse.route)
                        }
                    )
                }

                composable(Screen.CreateHouse.route) {
                    CreateHouseScreen(
                        onHouseCreated = { houseId ->
                            navController.navigate(Screen.HouseDetails.createRoute(houseId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.JoinHouse.route) {
                    `in`.xroden.flockr.ui.screens.home.JoinHouseScreen(
                        onHouseJoined = { houseId ->
                            navController.navigate(Screen.HouseDetails.createRoute(houseId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.HouseDetails.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    HouseDetailsScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToExpenses = { navController.navigate(Screen.Expenses.createRoute(houseId)) },
                        onNavigateToShopping = { navController.navigate(Screen.ShoppingList.createRoute(houseId)) },
                        onNavigateToChores = { navController.navigate(Screen.Chores.createRoute(houseId)) },
                        onNavigateToChat = { navController.navigate(Screen.Chat.createRoute(houseId)) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.createRoute(houseId)) },
                        onNavigateToManageMembers = {
                            android.util.Log.d("FlockrNavigation", "Navigating to ManageMembers for house: $houseId")
                            navController.navigate(Screen.ManageMembers.createRoute(houseId))
                        }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNotificationClick = { notification ->
                            // Deep link navigation based on notification data
                            val houseId = notification.houseId
                            when (notification.type) {
                                "expense", "settlement" -> {
                                    navController.navigate(Screen.Expenses.createRoute(houseId))
                                }
                                "shopping" -> {
                                    navController.navigate(Screen.ShoppingList.createRoute(houseId))
                                }
                                "chore" -> {
                                    navController.navigate(Screen.Chores.createRoute(houseId))
                                }
                                "message" -> {
                                    navController.navigate(Screen.Chat.createRoute(houseId))
                                }
                                "document" -> {
                                    navController.navigate(Screen.Documents.createRoute(houseId))
                                }
                                "per_diem" -> {
                                    navController.navigate(Screen.ExpenseDashboard.createRoute(houseId))
                                }
                                else -> {
                                    navController.navigate(Screen.HouseDetails.createRoute(houseId))
                                }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.Expenses.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    ExpensesScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onAddExpense = { navController.navigate(Screen.AddExpense.createRoute(houseId)) },
                        onNavigateToBalances = { navController.navigate(Screen.Balances.createRoute(houseId)) },
                        onNavigateToDashboard = { navController.navigate(Screen.ExpenseDashboard.createRoute(houseId)) }
                    )
                }

                composable(
                    route = Screen.AddExpense.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    AddExpenseScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onExpenseAdded = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ShoppingList.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    ShoppingListScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.createRoute(houseId)) }
                    )
                }

                composable(
                    route = Screen.Chores.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    ChoresScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    ChatScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ManageMembers.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    android.util.Log.d("FlockrNavigation", "ManageMembers screen loaded for house: $houseId")
                    `in`.xroden.flockr.ui.screens.house.ManageMembersScreen(
                        houseId = houseId,
                        onNavigateBack = {
                            android.util.Log.d("FlockrNavigation", "Navigating back from ManageMembers")
                            navController.popBackStack()
                        }
                    )
                }

                //
                // 3. FIXED: All the scrambled composable routes from the
                //    end of the original file are now correctly placed
                //    inside the 'Authenticated' NavHost.
                //

                composable(
                    route = Screen.Documents.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    DocumentsScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Balances.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    BalancesScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ExpenseDashboard.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    ExpenseDashboardScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPerDiemConfig = { navController.navigate(Screen.PerDiemConfig.createRoute(houseId)) }
                    )
                }

                composable(
                    route = Screen.PerDiemConfig.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.PerDiemConfigScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddEntry = { configId ->
                            navController.navigate("add_per_diem_entry/$houseId/$configId")
                        }
                    )
                }

                composable(
                    route = "add_per_diem_entry/{houseId}/{configId}",
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("configId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val configId = backStackEntry.arguments?.getString("configId") ?: return@composable
                    AddPerDiemEntryScreen(
                        houseId = houseId,
                        configId = configId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToProfile = { navController.navigate(Screen.EditProfile.route) },
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.EditProfile.route) {
                    `in`.xroden.flockr.ui.screens.profile.EditProfileScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                }
            }
        }
    }
}