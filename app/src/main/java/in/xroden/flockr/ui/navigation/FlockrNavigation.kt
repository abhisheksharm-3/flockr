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
import `in`.xroden.flockr.data.enums.NotificationType
import `in`.xroden.flockr.features.auth.ui.LoginScreen
import `in`.xroden.flockr.features.auth.ui.SignupScreen
import `in`.xroden.flockr.features.chat.ui.ChatScreen
import `in`.xroden.flockr.features.documents.ui.DocumentsScreen
import `in`.xroden.flockr.features.house.ui.home.CreateHouseScreen
import `in`.xroden.flockr.features.house.ui.home.HomeScreen
import `in`.xroden.flockr.features.house.ui.home.JoinHouseScreen
import `in`.xroden.flockr.features.house.ui.details.HouseDetailsScreen
import `in`.xroden.flockr.features.notifications.ui.NotificationScreen
import `in`.xroden.flockr.features.auth.ui.OnboardingScreen
import `in`.xroden.flockr.features.settings.ui.SettingsScreen
import `in`.xroden.flockr.features.auth.domain.AuthViewModel
import `in`.xroden.flockr.features.expenses.ui.dashboard.ExpenseDashboardScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.AddExpenseScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.BalancesScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.OneTimeExpensesScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.AddPerDiemEntryScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemConfigScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemTransactionsScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.QuickPerDiemEntryScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.AddRecurringExpenseScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.RecurringExpensesScreen
import `in`.xroden.flockr.features.expenses.ui.reports.MonthlyReportsScreen
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
                        `in`.xroden.flockr.features.auth.ui.WelcomeScreen(
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
                    JoinHouseScreen(
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
                                when (notification.type) {
                                    NotificationType.HOUSE_INVITE -> {
                                        // Navigate to home screen - user can see the invitation in notifications
                                        // Or we could show a dialog to accept/decline
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }
                                    NotificationType.EXPENSE, NotificationType.EXPENSE_SPLIT -> {
                                        navController.navigate(Screen.Expenses.createRoute(houseId))
                                    }
                                    NotificationType.SHOPPING -> {
                                        navController.navigate(Screen.ShoppingList.createRoute(houseId))
                                    }
                                    NotificationType.CHORE -> {
                                        navController.navigate(Screen.Chores.createRoute(houseId))
                                    }
                                    NotificationType.MESSAGE -> {
                                        navController.navigate(Screen.Chat.createRoute(houseId))
                                    }
                                    NotificationType.GENERAL -> {
                                        navController.navigate(Screen.Documents.createRoute(houseId))
                                    }
                                    NotificationType.PER_DIEM -> {
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
                    `in`.xroden.flockr.features.house.ui.settings.ManageMembersScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.HouseSettings.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    `in`.xroden.flockr.features.house.ui.settings.HouseSettingsScreen(
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
                    `in`.xroden.flockr.features.house.ui.settings.HouseAuditLogScreen(
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
                    PerDiemConfigScreen(
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

                // Modern Finance Screens
                composable(
                    route = Screen.OneTimeExpenses.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    OneTimeExpensesScreen(
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
                    AddExpenseScreen(
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
                    BalancesScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.RecurringExpenses.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    RecurringExpensesScreen(
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
                    AddRecurringExpenseScreen(
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
                    MonthlyReportsScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.QuickPerDiemEntry.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    QuickPerDiemEntryScreen(
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
                    PerDiemTransactionsScreen(
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
                    `in`.xroden.flockr.features.shopping.ui.ShoppingListScreen(
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
                    `in`.xroden.flockr.features.chores.ui.ChoresScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToProfile = { navController.navigate(Screen.EditProfile.route) },
                        onNavigateToNotificationPreferences = { navController.navigate(Screen.NotificationPreferences.route) },
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.NotificationPreferences.route) {
                    `in`.xroden.flockr.features.settings.ui.NotificationPreferencesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.EditProfile.route) {
                    `in`.xroden.flockr.features.settings.ui.EditProfileScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                }
            }
        }
    }
}