package `in`.xroden.flockr.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import `in`.xroden.flockr.data.enums.NotificationType
import `in`.xroden.flockr.features.chat.ui.ChatScreen
import `in`.xroden.flockr.features.chores.ui.AddChoreScreen
import `in`.xroden.flockr.features.chores.ui.ChoresScreen
import `in`.xroden.flockr.features.chores.ui.ProductivityScreen
import `in`.xroden.flockr.features.documents.ui.DocumentsScreen
import `in`.xroden.flockr.features.house.ui.details.HouseDetailsScreen
import `in`.xroden.flockr.features.house.ui.home.CreateHouseScreen
import `in`.xroden.flockr.features.house.ui.home.HomeScreen
import `in`.xroden.flockr.features.house.ui.home.JoinHousePreviewScreen
import `in`.xroden.flockr.features.house.ui.home.JoinHouseScreen
import `in`.xroden.flockr.features.house.ui.settings.HouseAuditLogScreen
import `in`.xroden.flockr.features.house.ui.settings.HouseSettingsScreen
import `in`.xroden.flockr.features.house.ui.settings.ManageMembersScreen
import `in`.xroden.flockr.features.notifications.ui.NotificationScreen
import `in`.xroden.flockr.features.shopping.ui.AddShoppingItemScreen
import `in`.xroden.flockr.features.shopping.ui.ShoppingListScreen
import org.json.JSONObject

fun NavGraphBuilder.houseGraph(navController: NavController) {
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
            onNavigateToJoinPreview = { inviteCode ->
                navController.navigate(JoinHousePreviewRoute(inviteCode))
            }
        )
    }

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
            onNavigateToExpenses = { navController.navigate(ExpenseDashboardRoute(route.houseId)) },
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
                            } catch (e: org.json.JSONException) {
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
                                navController.navigate(HomeRoute) {
                                    popUpTo<HomeRoute> { inclusive = true }
                                }
                            }
                        }
                        NotificationType.EXPENSE, NotificationType.EXPENSE_SPLIT -> {
                            navController.navigate(ExpenseDashboardRoute(houseId))
                        }
                        NotificationType.SHOPPING -> {
                            navController.navigate(ShoppingListRoute(houseId))
                        }
                        NotificationType.CHORE -> {
                            navController.navigate(ChoresRoute(houseId))
                        }
                        NotificationType.MESSAGE -> {
                            navController.navigate(ChatRoute(houseId))
                        }
                        NotificationType.GENERAL -> {
                            navController.navigate(DocumentsRoute(houseId))
                        }
                        NotificationType.PER_DIEM -> {
                            navController.navigate(ExpenseDashboardRoute(houseId))
                        }
                        else -> {
                            navController.navigate(HouseDetailsRoute(houseId))
                        }
                    }
                }
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

    // Organization / Features

    composable<ShoppingListRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ShoppingListRoute>()
        ShoppingListScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddItem = { navController.navigate(AddShoppingItemRoute(route.houseId)) },
            onNavigateToAddExpenseWithData = { itemName, quantity ->
                navController.navigate(AddExpenseAdvancedRoute(route.houseId, itemName, quantity))
            }
        )
    }

    composable<AddShoppingItemRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<AddShoppingItemRoute>()
        AddShoppingItemScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<ChoresRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ChoresRoute>()
        ChoresScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddChore = { navController.navigate(AddChoreRoute(route.houseId)) },
            onNavigateToProductivity = { navController.navigate(ProductivityRoute(route.houseId)) }
        )
    }

    composable<AddChoreRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<AddChoreRoute>()
        AddChoreScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable<ProductivityRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ProductivityRoute>()
        ProductivityScreen(
            houseId = route.houseId,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
