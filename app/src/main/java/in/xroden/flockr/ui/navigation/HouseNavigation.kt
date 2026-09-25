package `in`.xroden.flockr.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import `in`.xroden.flockr.features.chat.ui.ChatScreen
import `in`.xroden.flockr.features.chores.ui.ChoreFormScreen
import `in`.xroden.flockr.features.chores.ui.ChoresScreen
import `in`.xroden.flockr.features.documents.ui.DocumentsScreen
import `in`.xroden.flockr.features.house.ui.details.HouseDetailsScreen
import `in`.xroden.flockr.features.house.ui.home.CreateHouseScreen
import `in`.xroden.flockr.features.house.ui.home.JoinHousePreviewScreen
import `in`.xroden.flockr.features.house.ui.home.JoinHouseScreen
import `in`.xroden.flockr.features.house.ui.settings.HouseAuditLogScreen
import `in`.xroden.flockr.features.house.ui.settings.HouseSettingsScreen
import `in`.xroden.flockr.features.house.ui.settings.ManageMembersScreen
import `in`.xroden.flockr.features.shopping.ui.ShoppingListScreen

/** The house screens: joining and creating houses, a house's home, and its members, settings, chat, chores and lists. */
fun NavGraphBuilder.houseGraph(navController: NavController) {
    composable<CreateHouseRoute> {
        CreateHouseScreen(
            onHouseCreated = { houseId ->
                navController.navigate(HouseDetailsRoute(houseId)) {
                    popUpTo<HomeRoute>()
                }
            },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<JoinHouseRoute> {
        JoinHouseScreen(
            onHouseJoined = { houseId ->
                navController.navigate(HouseDetailsRoute(houseId)) {
                    popUpTo<HomeRoute>()
                }
            },
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<JoinHousePreviewRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<JoinHousePreviewRoute>()
        JoinHousePreviewScreen(
            inviteCode = route.inviteCode,
            onNavigateBack = { navController.popBackStack() },
            onHouseJoined = {
                navController.navigate(HomeRoute) {
                    popUpTo<HomeRoute> { inclusive = true }
                }
            }
        )
    }

    composable<HouseDetailsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<HouseDetailsRoute>()
        HouseDetailsScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToExpenses = { navController.navigate(ExpensesRoute(route.houseId)) },
            onNavigateToShopping = { navController.navigate(ShoppingListRoute(route.houseId)) },
            onNavigateToChores = { navController.navigate(ChoresRoute(route.houseId)) },
            onNavigateToChat = { navController.navigate(ChatRoute(route.houseId)) },
            onNavigateToDocuments = { navController.navigate(DocumentsRoute(route.houseId)) },
            onNavigateToManageMembers = {
                navController.navigate(ManageMembersRoute(route.houseId))
            },
            onNavigateToHouseSettings = {
                navController.navigate(HouseSettingsRoute(route.houseId))
            }
        )
    }

    composable<ChatRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ChatRoute>()
        ChatScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<ManageMembersRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ManageMembersRoute>()
        ManageMembersScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<HouseSettingsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<HouseSettingsRoute>()
        HouseSettingsScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAuditLog = {
                navController.navigate(HouseAuditLogRoute(route.houseId))
            },
            onDeleteHouse = {
                navController.navigate(HomeRoute) {
                    popUpTo<HomeRoute> { inclusive = false }
                    launchSingleTop = true
                }
            }
        )
    }

    composable<HouseAuditLogRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<HouseAuditLogRoute>()
        HouseAuditLogScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<DocumentsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<DocumentsRoute>()
        DocumentsScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<ShoppingListRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ShoppingListRoute>()
        ShoppingListScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onRecordExpense = { itemName -> navController.navigate(ExpenseFormRoute(route.houseId, prefillName = itemName)) },
        )
    }

    composable<ChoresRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ChoresRoute>()
        ChoresScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onAddChore = { navController.navigate(ChoreFormRoute(route.houseId)) },
            onEditChore = { navController.navigate(ChoreFormRoute(route.houseId, it)) },
        )
    }

    composable<ChoreFormRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ChoreFormRoute>()
        ChoreFormScreen(
            houseId = route.houseId,
            choreId = route.choreId,
            onNavigateBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
        )
    }
}
