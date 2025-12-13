package `in`.xroden.flockr.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import `in`.xroden.flockr.features.expenses.ui.perdiem.AddPerDiemConfigScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.EditPerDiemConfigScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.AddRecurringExpenseScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.RecurringExpensesScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.ExpenseDetailScreen
import `in`.xroden.flockr.features.chores.ui.AddChoreScreen
import `in`.xroden.flockr.features.chores.ui.ProductivityScreen
import `in`.xroden.flockr.features.shopping.ui.AddShoppingItemScreen

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





    //
    // 2. FIXED: The 'when' block was syntactically incorrect.
    //    It is now properly structured with 'is' cases.
    //
    // State to track if we have successfully loaded the authenticated graph at least once
    // This allows us to keep the NavHost in composition during transient "Loading" states
    // (e.g. returning from file picker) preventing the destruction of the DocumentsScreen
    val hasAuthenticatedSession = remember { mutableStateOf(false) }

    // Update our tracking state
    LaunchedEffect(authUiState) {
        if (authUiState is AuthNavigationState.Authenticated) {
            hasAuthenticatedSession.value = true
        } else if (authUiState is AuthNavigationState.Unauthenticated || authUiState is AuthNavigationState.NeedsOnboarding) {
            hasAuthenticatedSession.value = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Authenticated Content (Main App)
        // We show this if we are currently authenticated OR if we were authenticated and are just in a transient loading state
        if (authUiState is AuthNavigationState.Authenticated || (hasAuthenticatedSession.value && authUiState is AuthNavigationState.Loading)) {
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
                    val context = androidx.compose.ui.platform.LocalContext.current
                    NotificationScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNotificationClick = { notification ->
                            // Deep link navigation based on notification data
                            val houseId = notification.houseId
                            if (houseId != null) {
                                when (notification.type) {
                                    NotificationType.HOUSE_INVITE -> {
                        // Launch deep link intentionally to trigger MainActivity's handling
                        // This allows the global dialog to appear even from within the app
                        val inviteCode = try {
                            val data = notification.data
                            if (!data.isNullOrEmpty()) {
                                val json = org.json.JSONObject(data)
                                json.optString("invite_code").takeIf { it.isNotEmpty() }
                                    ?: json.optString("code").takeIf { it.isNotEmpty() }
                            } else null
                        } catch (e: Exception) {
                            null
                        }

                        if (!inviteCode.isNullOrEmpty()) {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("flockr://invite/$inviteCode")
                            )
                            intent.setPackage(context.packageName)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } else {
                            // Fallback to home if no code
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
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
                        onNavigateToReports = { navController.navigate(Screen.MonthlyReports.createRoute(houseId)) },
                        onNavigateToExpenseDetail = { expenseId ->
                            navController.navigate("expense_detail/$houseId/$expenseId")
                        }
                    )
                }

                // One-time expenses list (supports optional query params)
                composable(
                    route = Screen.OneTimeExpenses.route,
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("category") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("userId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val category = backStackEntry.arguments?.getString("category")
                    val userId = backStackEntry.arguments?.getString("userId")

                    OneTimeExpensesScreen(
                        houseId = houseId,
                        initialCategory = category,
                        initialUserId = userId,
                        onNavigateBack = { navController.popBackStack() },
                        onAddExpense = { navController.navigate(Screen.AddExpense.createRoute(houseId)) },
                        onNavigateToExpenseDetail = { expenseId -> navController.navigate("expense_detail/$houseId/$expenseId") },
                        onNavigateToEditExpense = { expenseId -> navController.navigate("edit_expense/$houseId/$expenseId") }
                    )
                }

                // Add expense (simple route)
                composable(
                    route = Screen.AddExpense.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    AddExpenseScreen(
                        houseId = houseId,
                        initialName = null,
                        initialQuantity = null,
                        onNavigateBack = { navController.popBackStack() },
                        onExpenseAdded = { navController.popBackStack() }
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
                        },
                        onNavigateToAddConfig = {
                            navController.navigate(Screen.AddPerDiemConfig.createRoute(houseId))
                        },
                        onNavigateToEditConfig = { config ->
                            navController.navigate(Screen.EditPerDiemConfig.createRoute(houseId, config))
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
                    route = Screen.AddExpenseAdvanced.route,
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
                        },
                        onNavigateToEditBill = { expenseId ->
                            navController.navigate("edit_recurring_expense/$houseId/$expenseId")
                        },
                        onNavigateToHistory = { expenseId, expenseName ->
                            navController.navigate(Screen.BillHistory.createRoute(houseId, expenseId, expenseName))
                        }
                    )
                }

                composable(
                    route = Screen.BillHistory.route,
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("expenseId") { type = NavType.StringType },
                        navArgument("expenseName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                    val expenseName = backStackEntry.arguments?.getString("expenseName") ?: return@composable
                    `in`.xroden.flockr.features.expenses.ui.recurring.BillHistoryScreen(
                        houseId = houseId,
                        recurringExpenseId = expenseId,
                        expenseName = expenseName,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "edit_expense/{houseId}/{expenseId}",
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("expenseId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                    
                    `in`.xroden.flockr.features.expenses.ui.onetime.EditExpenseScreen(
                        houseId = houseId,
                        expenseId = expenseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "expense_detail/{houseId}/{expenseId}",
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("expenseId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                    ExpenseDetailScreen(
                        houseId = houseId,
                        expenseId = expenseId,
                        onNavigateBack = { navController.popBackStack() },
                        onEditExpense = { id -> 
                            navController.navigate("edit_expense/$houseId/$id")
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
                    route = "edit_recurring_expense/{houseId}/{expenseId}",
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("expenseId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                    
                    `in`.xroden.flockr.features.expenses.ui.recurring.EditRecurringExpenseScreen(
                        houseId = houseId,
                        expenseId = expenseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.MonthlyReports.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    MonthlyReportsScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCategory = { category ->
                            navController.navigate(Screen.OneTimeExpenses.createRoute(houseId, category = category))
                        },
                        onNavigateToUser = { userId ->
                            navController.navigate(Screen.OneTimeExpenses.createRoute(houseId, userId = userId))
                        },
                        onNavigateToOneTimeExpenses = {
                            navController.navigate(Screen.OneTimeExpenses.createRoute(houseId))
                        },
                        onNavigateToRecurringExpenses = {
                            navController.navigate(Screen.RecurringExpenses.createRoute(houseId))
                        },
                        onNavigateToPerDiemExpenses = {
                            navController.navigate(Screen.PerDiemTransactions.createRoute(houseId))
                        }
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
                    route = Screen.PerDiemConfig.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    PerDiemConfigScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddEntry = { configId ->
                            navController.navigate("add_per_diem_entry/$houseId/$configId")
                        },
                        onNavigateToAddConfig = {
                            navController.navigate(Screen.AddPerDiemConfig.createRoute(houseId))
                        },
                        onNavigateToEditConfig = { config ->
                            navController.navigate(Screen.EditPerDiemConfig.createRoute(houseId, config))
                        }
                    )
                }

                composable(
                    route = Screen.AddPerDiemConfig.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    AddPerDiemConfigScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.EditPerDiemConfig.route,
                    arguments = listOf(
                        navArgument("houseId") { type = NavType.StringType },
                        navArgument("configId") { type = NavType.StringType },
                        navArgument("itemName") { type = NavType.StringType },
                        navArgument("rate") { type = NavType.StringType },
                        navArgument("category") { type = NavType.StringType },
                        navArgument("unit") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    val configId = backStackEntry.arguments?.getString("configId") ?: return@composable
                    val itemName = backStackEntry.arguments?.getString("itemName") ?: ""
                    val rate = backStackEntry.arguments?.getString("rate") ?: ""
                    val category = backStackEntry.arguments?.getString("category") ?: ""
                    val unit = backStackEntry.arguments?.getString("unit") ?: ""

                    EditPerDiemConfigScreen(
                        houseId = houseId,
                        configId = configId,
                        initialItemName = itemName,
                        initialRate = rate,
                        initialCategory = category,
                        initialUnit = unit,
                        onNavigateBack = { navController.popBackStack() }
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
                        onNavigateToAddItem = { navController.navigate(Screen.AddShoppingItem.createRoute(houseId)) },
                        onNavigateToAddExpenseWithData = { itemName, quantity ->
                            navController.navigate(Screen.AddExpenseAdvanced.createRoute(houseId, itemName, quantity))
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
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddChore = { navController.navigate(Screen.AddChore.createRoute(houseId)) },
                        onNavigateToProductivity = { navController.navigate(Screen.Productivity.createRoute(houseId)) }
                    )
                }

                composable(
                    route = Screen.AddChore.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    AddChoreScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Productivity.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    ProductivityScreen(
                        houseId = houseId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.AddShoppingItem.route,
                    arguments = listOf(navArgument("houseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val houseId = backStackEntry.arguments?.getString("houseId") ?: return@composable
                    AddShoppingItemScreen(
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
                        },
                        onNavigateToSecurity = {
                            navController.navigate(Screen.SecuritySettings.route)
                        }
                    )
                }

                composable(Screen.SecuritySettings.route) {
                    `in`.xroden.flockr.features.settings.ui.SecuritySettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
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

        // Unauthenticated Content
        if (authUiState is AuthNavigationState.Unauthenticated) {
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

        // Onboarding Content
        if (authUiState is AuthNavigationState.NeedsOnboarding) {
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

        // Loading Overlay
        // Show this if explicitly loading AND we don't have a persisted session (or we want to show a spinner on top)
        // OR better: show it only if we are truly in a loading state that shouldn't show content.
        // For transient loading (hasAuthenticatedSession = true), we might want to show a small indicator or nothing.
        // For now, let's show the full screen loader only if we have NO content to show.
        if (authUiState is AuthNavigationState.Loading && !hasAuthenticatedSession.value) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (authUiState is AuthNavigationState.Loading && hasAuthenticatedSession.value) {
             // Optional: Show a non-blocking loading indicator for transient states?
             // For document upload, we prefer nothing to disturb the UI.
        }
    }