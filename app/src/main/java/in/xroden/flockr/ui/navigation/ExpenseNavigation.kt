package `in`.xroden.flockr.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import `in`.xroden.flockr.features.expenses.ui.dashboard.ExpenseDashboardScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.AddExpenseScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.BalancesScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.EditExpenseScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.ExpenseDetailScreen
import `in`.xroden.flockr.features.expenses.ui.onetime.OneTimeExpensesScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.AddPerDiemConfigScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.AddPerDiemEntryScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.EditPerDiemConfigScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemConfigScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemTransactionsScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.QuickPerDiemEntryScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.AddRecurringExpenseScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.BillHistoryScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.EditRecurringExpenseScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.RecurringExpensesScreen
import `in`.xroden.flockr.features.expenses.ui.reports.MonthlyReportsScreen

fun NavGraphBuilder.expenseGraph(navController: NavController) {
    composable<ExpenseDashboardRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ExpenseDashboardRoute>()
        ExpenseDashboardScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToOneTimeExpenses = { navController.navigate(OneTimeExpensesRoute(route.houseId)) },
            onNavigateToRecurringExpenses = { navController.navigate(RecurringExpensesRoute(route.houseId)) },
            onNavigateToBalances = { navController.navigate(BalancesRoute(route.houseId)) },
            onNavigateToPerDiem = { navController.navigate(PerDiemConfigRoute(route.houseId)) },
            onNavigateToQuickPerDiem = { navController.navigate(QuickPerDiemEntryRoute(route.houseId)) },
            onNavigateToReports = { navController.navigate(MonthlyReportsRoute(route.houseId)) },
            onNavigateToExpenseDetail = { expenseId ->
                navController.navigate(ExpenseDetailRoute(route.houseId, expenseId))
            }
        )
    }

    composable<OneTimeExpensesRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<OneTimeExpensesRoute>()
        OneTimeExpensesScreen(
            houseId = route.houseId,
            initialCategory = route.category,
            initialUserId = route.userId,
            onNavigateBack = { navController.popBackStack() },
            onAddExpense = { navController.navigate(AddExpenseRoute(route.houseId)) },
            onNavigateToExpenseDetail = { expenseId -> 
                navController.navigate(ExpenseDetailRoute(route.houseId, expenseId)) 
            },
            onNavigateToEditExpense = { expenseId -> 
                navController.navigate(EditExpenseRoute(route.houseId, expenseId)) 
            }
        )
    }

    composable<AddExpenseRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<AddExpenseRoute>()
        AddExpenseScreen(
            houseId = route.houseId,
            initialName = null,
            initialQuantity = null,
            onNavigateBack = { navController.popBackStack() },
            onExpenseAdded = { navController.popBackStack() }
        )
    }

    composable<AddExpenseAdvancedRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<AddExpenseAdvancedRoute>()
        AddExpenseScreen(
            houseId = route.houseId,
            initialName = route.itemName,
            initialQuantity = route.quantity,
            onNavigateBack = { navController.popBackStack() },
            onExpenseAdded = { navController.popBackStack() }
        )
    }

    composable<BalancesRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<BalancesRoute>()
        BalancesScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<RecurringExpensesRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<RecurringExpensesRoute>()
        RecurringExpensesScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddBill = {
                navController.navigate(AddRecurringExpenseRoute(route.houseId))
            },
            onNavigateToEditBill = { expenseId ->
                navController.navigate(EditRecurringExpenseRoute(route.houseId, expenseId))
            },
            onNavigateToHistory = { expenseId, expenseName ->
                navController.navigate(BillHistoryRoute(route.houseId, expenseId, expenseName))
            }
        )
    }

    composable<BillHistoryRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<BillHistoryRoute>()
        BillHistoryScreen(
            houseId = route.houseId,
            recurringExpenseId = route.expenseId,
            expenseName = route.expenseName,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<EditExpenseRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<EditExpenseRoute>()
        EditExpenseScreen(
            houseId = route.houseId,
            expenseId = route.expenseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<ExpenseDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ExpenseDetailRoute>()
        ExpenseDetailScreen(
            houseId = route.houseId,
            expenseId = route.expenseId,
            onNavigateBack = { navController.popBackStack() },
            onEditExpense = { id ->
                navController.navigate(EditExpenseRoute(route.houseId, id))
            }
        )
    }

    composable<AddRecurringExpenseRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<AddRecurringExpenseRoute>()
        AddRecurringExpenseScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onExpenseAdded = { navController.popBackStack() }
        )
    }

    composable<EditRecurringExpenseRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<EditRecurringExpenseRoute>()
        EditRecurringExpenseScreen(
            houseId = route.houseId,
            expenseId = route.expenseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<MonthlyReportsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MonthlyReportsRoute>()
        MonthlyReportsScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToCategory = { category ->
                navController.navigate(OneTimeExpensesRoute(route.houseId, category = category))
            },
            onNavigateToUser = { userId ->
                navController.navigate(OneTimeExpensesRoute(route.houseId, userId = userId))
            },
            onNavigateToOneTimeExpenses = {
                navController.navigate(OneTimeExpensesRoute(route.houseId))
            },
            onNavigateToRecurringExpenses = {
                navController.navigate(RecurringExpensesRoute(route.houseId))
            },
            onNavigateToPerDiemExpenses = {
                navController.navigate(PerDiemTransactionsRoute(route.houseId))
            }
        )
    }

    composable<QuickPerDiemEntryRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<QuickPerDiemEntryRoute>()
        QuickPerDiemEntryScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddEntry = { configId ->
                navController.navigate(AddPerDiemEntryRoute(route.houseId, configId))
            },
            onNavigateToConfig = {
                navController.navigate(PerDiemConfigRoute(route.houseId))
            },
            onNavigateToTransactions = {
                navController.navigate(PerDiemTransactionsRoute(route.houseId))
            }
        )
    }

    composable<PerDiemConfigRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PerDiemConfigRoute>()
        PerDiemConfigScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddEntry = { configId ->
                navController.navigate(AddPerDiemEntryRoute(route.houseId, configId))
            },
            onNavigateToAddConfig = {
                navController.navigate(AddPerDiemConfigRoute(route.houseId))
            },
            onNavigateToEditConfig = { config ->
                navController.navigate(EditPerDiemConfigRoute(
                    houseId = route.houseId,
                    configId = config.id,
                    itemName = config.itemName,
                    rate = config.rate.toDouble(),
                    category = config.category,
                    unit = config.unit
                ))
            }
        )
    }

    composable<AddPerDiemConfigRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<AddPerDiemConfigRoute>()
        AddPerDiemConfigScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<EditPerDiemConfigRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<EditPerDiemConfigRoute>()
        EditPerDiemConfigScreen(
            houseId = route.houseId,
            configId = route.configId,
            initialItemName = route.itemName,
            initialRate = route.rate.toString(),
            initialCategory = route.category,
            initialUnit = route.unit,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<AddPerDiemEntryRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<AddPerDiemEntryRoute>()
        AddPerDiemEntryScreen(
            houseId = route.houseId,
            configId = route.configId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<PerDiemTransactionsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PerDiemTransactionsRoute>()
        PerDiemTransactionsScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
