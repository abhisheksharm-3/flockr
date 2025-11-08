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
import `in`.xroden.flockr.ui.screens.documents.DocumentsScreen
import `in`.xroden.flockr.ui.screens.expenses.ExpenseDashboardScreen
import `in`.xroden.flockr.ui.screens.home.CreateHouseScreen
import `in`.xroden.flockr.ui.screens.home.HomeScreen
import `in`.xroden.flockr.ui.screens.home.JoinHouseScreen
import `in`.xroden.flockr.ui.screens.house.HouseDetailsScreen
import `in`.xroden.flockr.ui.screens.notifications.NotificationScreen
import `in`.xroden.flockr.ui.screens.onboarding.OnboardingScreen
import `in`.xroden.flockr.ui.screens.settings.SettingsScreen
import `in`.xroden.flockr.ui.viewmodel.AuthViewModel
import io.github.jan.supabase.gotrue.SessionStatus

/**
 * Navigation component for Flockr app with authentication state management.
 */
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
    val sessionStatus by authViewModel.sessionStatus.collectAsState(initial = SessionStatus.LoadingFromStorage)
    val profile by authViewModel.profile.collectAsState(initial = null)
    val authUiState by authViewModel.authNavigationState.collectAsState(initial = AuthNavigationState.Loading)

    androidx.compose.runtime.LaunchedEffect(authUiState) {
        android.util.Log.i("FlockrNavigation", "🎯 Auth state changed: ${authUiState::class.simpleName}")
        when (authUiState) {
            is AuthNavigationState.Loading -> android.util.Log.d("FlockrNavigation", "Loading authentication state...")
            is AuthNavigationState.Unauthenticated -> android.util.Log.d("FlockrNavigation", "User not authenticated, showing login")
            is AuthNavigationState.NeedsOnboarding -> android.util.Log.d("FlockrNavigation", "User needs onboarding")
            is AuthNavigationState.Authenticated -> android.util.Log.d("FlockrNavigation", "User authenticated, profile=${profile?.fullName}")
        }
    }

    androidx.compose.runtime.DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, arguments ->
            android.util.Log.i("FlockrNavigation", "🧭 Navigation: → ${destination.route}")
            arguments?.keySet()?.forEach { key ->
                android.util.Log.d("FlockrNavigation", "   └─ $key = ${arguments.get(key)}")
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
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
                    startDestination = Screen.Welcome.route
                ) {
                    composable(Screen.Welcome.route) {
                        `in`.xroden.flockr.ui.screens.welcome.WelcomeScreen(
                            onGetStarted = {
                                navController.navigate(Screen.Signup.route)
                            },
                            onSignIn = {
                                navController.navigate(Screen.Login.route)
                            }
                            // Now uses local images: welcome_bg_light.jpg / welcome_bg_dark.jpg
                            // Place your images in: app/src/main/res/drawable/
                        )
                    }

                    composable(Screen.Login.route) {
                        LoginScreen(
                            onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                            onNavigateBack = { navController.popBackStack() }
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
                        onNavigateToExpenses = { navController.navigate(Screen.ExpenseDashboard.createRoute(houseId)) },
                        onNavigateToShopping = { navController.navigate(Screen.ShoppingList.createRoute(houseId)) },
                        onNavigateToChores = { navController.navigate(Screen.Chores.createRoute(houseId)) },
                        onNavigateToChat = { navController.navigate(Screen.Chat.createRoute(houseId)) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.createRoute(houseId)) },
                        onNavigateToManageMembers = {
                            navController.navigate(Screen.ManageMembers.createRoute(houseId))
                        },
                        onNavigateToHouseSettings = {
                            navController.navigate(Screen.HouseSettings.createRoute(houseId))
                        }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNotificationClick = { notification ->
                            // Deep link navigation based on notification data
                            val houseId = notification.houseId
                            if (houseId != null) {
                                when (notification.notificationType) {
                                    "house_invitation" -> {
                                        // Navigate to home screen - user can see the invitation in notifications
                                        // Or we could show a dialog to accept/decline
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }
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
                        }
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
                    `in`.xroden.flockr.ui.screens.house.ManageMembersScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.HouseSettings.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.house.HouseSettingsScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAuditLog = {
                            navController.navigate(Screen.HouseAuditLog.createRoute(houseId))
                        },
                        onDeleteHouse = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(
                    route = Screen.HouseAuditLog.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.house.HouseAuditLogScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

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
                    route = Screen.ExpenseDashboard.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    ExpenseDashboardScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToOneTimeExpenses = { navController.navigate(Screen.OneTimeExpenses.createRoute(houseId)) },
                        onNavigateToRecurringExpenses = { navController.navigate(Screen.RecurringExpenses.createRoute(houseId)) },
                        onNavigateToBalances = { navController.navigate(Screen.Balances.createRoute(houseId)) },
                        onNavigateToPerDiem = { navController.navigate(Screen.PerDiemConfig.createRoute(houseId)) },
                        onNavigateToQuickPerDiem = { navController.navigate(Screen.QuickPerDiemEntry.createRoute(houseId)) },
                        onNavigateToReports = { navController.navigate(Screen.MonthlyReports.createRoute(houseId)) }
                    )
                }

                composable(
                    route = Screen.PerDiemConfig.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.PerDiemConfigScreenModern(
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
                    `in`.xroden.flockr.ui.screens.expenses.AddPerDiemEntryScreenModern(
                        houseId = houseId,
                        configId = configId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Modern Finance Screens
                composable(
                    route = Screen.OneTimeExpenses.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.OneTimeExpensesScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onAddExpense = { navController.navigate(Screen.AddExpense.createRoute(houseId)) }
                    )
                }

                composable(
                    route = Screen.AddExpense.route,
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("itemName") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("quantity") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val itemName = backStackEntry.arguments?.getString("itemName")
                    val quantityStr = backStackEntry.arguments?.getString("quantity")
                    val quantity = quantityStr?.toIntOrNull()
                    `in`.xroden.flockr.ui.screens.expenses.AddExpenseScreenModern(
                        houseId = houseId,
                        initialName = itemName,
                        initialQuantity = quantity,
                        onNavigateBack = { navController.popBackStack() },
                        onExpenseAdded = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Balances.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.BalancesScreenModern(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.RecurringExpenses.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.RecurringExpensesScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddBill = {
                            navController.navigate(Screen.AddRecurringExpense.createRoute(houseId))
                        }
                    )
                }

                composable(
                    route = Screen.AddRecurringExpense.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.AddRecurringExpenseScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onExpenseAdded = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.MonthlyReports.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.MonthlyReportsScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.QuickPerDiemEntry.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.QuickPerDiemEntryScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddEntry = { configId ->
                            navController.navigate("add_per_diem_entry/$houseId/$configId")
                        },
                        onNavigateToConfig = {
                            navController.navigate(Screen.PerDiemConfig.createRoute(houseId))
                        },
                        onNavigateToTransactions = {
                            navController.navigate(Screen.PerDiemTransactions.createRoute(houseId))
                        }
                    )
                }

                composable(
                    route = Screen.PerDiemTransactions.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.expenses.PerDiemTransactionsScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Organization Screens
                composable(
                    route = Screen.ShoppingList.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.shopping.ShoppingListScreenModern(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.createRoute(houseId)) },
                        onNavigateToAddExpenseWithData = { _: String, _: Int ->
                            navController.navigate(Screen.AddExpense.createRoute(houseId))
                        }
                    )
                }

                composable(
                    route = Screen.Chores.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.ui.screens.chores.ChoresScreenModern(
                        houseId = houseId,
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