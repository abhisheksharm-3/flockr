/** The expense screens and how they lead to one another. */
package `in`.xroden.flockr.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import `in`.xroden.flockr.features.expenses.ui.ledger.BalancesScreen
import `in`.xroden.flockr.features.expenses.ui.ledger.ExpenseDetailScreen
import `in`.xroden.flockr.features.expenses.ui.ledger.ExpenseFormScreen
import `in`.xroden.flockr.features.expenses.ui.ledger.ExpensesNavigation
import `in`.xroden.flockr.features.expenses.ui.ledger.ExpensesScreen
import `in`.xroden.flockr.features.expenses.ui.ledger.SettleUpScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemEntryFormScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemItemFormScreen
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemNavigation
import `in`.xroden.flockr.features.expenses.ui.perdiem.PerDiemScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.BillFormScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.BillHistoryScreen
import `in`.xroden.flockr.features.expenses.ui.recurring.BillsScreen
import `in`.xroden.flockr.features.expenses.ui.reports.ReportsScreen

fun NavGraphBuilder.expenseGraph(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }

    composable<ExpensesRoute> { entry ->
        val houseId = entry.toRoute<ExpensesRoute>().houseId
        ExpensesScreen(
            houseId = houseId,
            navigation = ExpensesNavigation(
                back = back,
                addExpense = { navController.navigate(ExpenseFormRoute(houseId)) },
                openExpense = { navController.navigate(ExpenseDetailRoute(houseId, it)) },
                settleUp = { from, to, amount -> navController.navigate(SettleUpRoute(houseId, from, to, amount?.toPlainString())) },
                balances = { navController.navigate(BalancesRoute(houseId)) },
                bills = { navController.navigate(BillsRoute(houseId)) },
                perDiem = { navController.navigate(PerDiemRoute(houseId)) },
                reports = { navController.navigate(ReportsRoute(houseId)) },
            ),
        )
    }
    composable<ExpenseFormRoute> { entry ->
        val route = entry.toRoute<ExpenseFormRoute>()
        ExpenseFormScreen(
            houseId = route.houseId,
            expenseId = route.expenseId,
            initialName = route.prefillName,
            initialQuantity = route.prefillQuantity,
            onNavigateBack = back,
            onSaved = back,
        )
    }
    composable<ExpenseDetailRoute> { entry ->
        val route = entry.toRoute<ExpenseDetailRoute>()
        ExpenseDetailScreen(
            houseId = route.houseId,
            expenseId = route.expenseId,
            onNavigateBack = back,
            onEdit = { navController.navigate(ExpenseFormRoute(route.houseId, expenseId = it)) },
        )
    }
    composable<BalancesRoute> { entry ->
        val houseId = entry.toRoute<BalancesRoute>().houseId
        BalancesScreen(
            houseId = houseId,
            onNavigateBack = back,
            onSettleUp = { from, to, amount -> navController.navigate(SettleUpRoute(houseId, from, to, amount.toPlainString())) },
            onOpenExpense = { navController.navigate(ExpenseDetailRoute(houseId, it)) },
        )
    }
    composable<SettleUpRoute> { entry ->
        val route = entry.toRoute<SettleUpRoute>()
        SettleUpScreen(
            houseId = route.houseId,
            fromUserId = route.fromUserId,
            toUserId = route.toUserId,
            amount = route.amount?.toBigDecimalOrNull(),
            onNavigateBack = back,
            onSaved = back,
        )
    }
    composable<BillsRoute> { entry ->
        val houseId = entry.toRoute<BillsRoute>().houseId
        BillsScreen(
            houseId = houseId,
            onNavigateBack = back,
            onAddBill = { navController.navigate(BillFormRoute(houseId)) },
            onEditBill = { navController.navigate(BillFormRoute(houseId, it)) },
            onBillHistory = { navController.navigate(BillHistoryRoute(houseId, it)) },
        )
    }
    composable<BillFormRoute> { entry ->
        val route = entry.toRoute<BillFormRoute>()
        BillFormScreen(houseId = route.houseId, billId = route.billId, onNavigateBack = back, onSaved = back)
    }
    composable<BillHistoryRoute> { entry ->
        val route = entry.toRoute<BillHistoryRoute>()
        BillHistoryScreen(
            houseId = route.houseId,
            billId = route.billId,
            onNavigateBack = back,
            onOpenExpense = { navController.navigate(ExpenseDetailRoute(route.houseId, it)) },
        )
    }
    composable<ReportsRoute> { entry ->
        ReportsScreen(houseId = entry.toRoute<ReportsRoute>().houseId, onNavigateBack = back)
    }
    composable<PerDiemRoute> { entry ->
        val houseId = entry.toRoute<PerDiemRoute>().houseId
        PerDiemScreen(
            houseId = houseId,
            navigation = PerDiemNavigation(
                back = back,
                logUsage = { navController.navigate(PerDiemEntryFormRoute(houseId, it)) },
                addItem = { navController.navigate(PerDiemItemFormRoute(houseId)) },
                editItem = { navController.navigate(PerDiemItemFormRoute(houseId, it)) },
                openExpense = { navController.navigate(ExpenseDetailRoute(houseId, it)) },
            ),
        )
    }
    composable<PerDiemItemFormRoute> { entry ->
        val route = entry.toRoute<PerDiemItemFormRoute>()
        PerDiemItemFormScreen(houseId = route.houseId, configId = route.configId, onNavigateBack = back, onDone = back)
    }
    composable<PerDiemEntryFormRoute> { entry ->
        val route = entry.toRoute<PerDiemEntryFormRoute>()
        PerDiemEntryFormScreen(
            houseId = route.houseId,
            configId = route.configId,
            onNavigateBack = back,
            onSaved = back,
            onAddItem = { navController.navigate(PerDiemItemFormRoute(route.houseId)) },
        )
    }
}
