package `in`.xroden.flockr.ui.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object HouseDetails : Screen("house_details/{houseId}") {
        fun createRoute(houseId: String) = "house_details/$houseId"
    }
    object Notifications : Screen("notifications")
    object Expenses : Screen("expenses/{houseId}") {
        fun createRoute(houseId: String) = "expenses/$houseId"
    }
    object AddExpense : Screen("add_expense/{houseId}") {
        fun createRoute(houseId: String) = "add_expense/$houseId"
    }
    object ShoppingList : Screen("shopping/{houseId}") {
        fun createRoute(houseId: String) = "shopping/$houseId"
    }
    object Chores : Screen("chores/{houseId}") {
        fun createRoute(houseId: String) = "chores/$houseId"
    }
    object Chat : Screen("chat/{houseId}") {
        fun createRoute(houseId: String) = "chat/$houseId"
    }
    object Documents : Screen("documents/{houseId}") {
        fun createRoute(houseId: String) = "documents/$houseId"
    }
    object Balances : Screen("balances/{houseId}") {
        fun createRoute(houseId: String) = "balances/$houseId"
    }
    object ExpenseDashboard : Screen("expense_dashboard/{houseId}") {
        fun createRoute(houseId: String) = "expense_dashboard/$houseId"
    }
    object AddPerDiemEntry : Screen("add_per_diem/{houseId}") {
        fun createRoute(houseId: String) = "add_per_diem/$houseId"
    }
    object Settings : Screen("settings")
    object CreateHouse : Screen("create_house")
    object JoinHouse : Screen("join_house")
    object ManageMembers : Screen("manage_members/{houseId}") {
        fun createRoute(houseId: String) = "manage_members/$houseId"
    }
    object HouseSettings : Screen("house_settings/{houseId}") {
        fun createRoute(houseId: String) = "house_settings/$houseId"
    }
    object PerDiemConfig : Screen("per_diem_config/{houseId}") {
        fun createRoute(houseId: String) = "per_diem_config/$houseId"
    }
    object EditProfile : Screen("edit_profile")
    
    // Finance Screens
    object OneTimeExpenses : Screen("one_time_expenses/{houseId}?category={category}&userId={userId}") {
        fun createRoute(houseId: String, category: String? = null, userId: String? = null): String {
            var route = "one_time_expenses/$houseId"
            val params = mutableListOf<String>()
            if (category != null) params.add("category=$category")
            if (userId != null) params.add("userId=$userId")
            
            if (params.isNotEmpty()) {
                route += "?${params.joinToString("&")}"
            }
            return route
        }
    }
    object RecurringExpenses : Screen("recurring_expenses/{houseId}") {
        fun createRoute(houseId: String) = "recurring_expenses/$houseId"
    }
    object AddRecurringExpense : Screen("add_recurring_expense/{houseId}") {
        fun createRoute(houseId: String) = "add_recurring_expense/$houseId"
    }
    object BillHistory : Screen("bill_history/{houseId}/{expenseId}/{expenseName}") {
        fun createRoute(houseId: String, expenseId: String, expenseName: String) = "bill_history/$houseId/$expenseId/$expenseName"
    }
    object MonthlyReports : Screen("monthly_reports/{houseId}") {
        fun createRoute(houseId: String) = "monthly_reports/$houseId"
    }
    object AddExpenseAdvanced : Screen("add_expense_advanced/{houseId}?itemName={itemName}&quantity={quantity}") {
        fun createRoute(houseId: String, itemName: String? = null, quantity: Int? = null): String {
            var route = "add_expense_advanced/$houseId"
            if (itemName != null) {
                route += "?itemName=${java.net.URLEncoder.encode(itemName, "UTF-8")}"
                if (quantity != null) {
                    route += "&quantity=$quantity"
                }
            }
            return route
        }
    }
    object BalancesDetailed : Screen("balances_detailed/{houseId}") {
        fun createRoute(houseId: String) = "balances_detailed/$houseId"
    }
    object QuickPerDiemEntry : Screen("quick_per_diem/{houseId}") {
        fun createRoute(houseId: String) = "quick_per_diem/$houseId"
    }
    object PerDiemTransactions : Screen("per_diem_transactions/{houseId}") {
        fun createRoute(houseId: String) = "per_diem_transactions/$houseId"
    }

    // Organization Screens
    object ShoppingListDetailed : Screen("shopping_detailed/{houseId}") {
        fun createRoute(houseId: String) = "shopping_detailed/$houseId"
    }
    object ChoresDetailed : Screen("chores_detailed/{houseId}") {
        fun createRoute(houseId: String) = "chores_detailed/$houseId"
    }

    // New Feature Screens
    object NotificationPreferences : Screen("notification_preferences")
    object HouseAuditLog : Screen("house_audit_log/{houseId}") {
        fun createRoute(houseId: String) = "house_audit_log/$houseId"
    }
    object AddChore : Screen("add_chore/{houseId}") {
        fun createRoute(houseId: String) = "add_chore/$houseId"
    }
    object Productivity : Screen("productivity/{houseId}") {
        fun createRoute(houseId: String) = "productivity/$houseId"
    }
    object AddShoppingItem : Screen("add_shopping_item/{houseId}") {
        fun createRoute(houseId: String) = "add_shopping_item/$houseId"
    }

    object AddPerDiemConfig : Screen("add_per_diem_config/{houseId}") {
        fun createRoute(houseId: String) = "add_per_diem_config/$houseId"
    }

    object EditPerDiemConfig : Screen("edit_per_diem_config/{houseId}/{configId}?itemName={itemName}&rate={rate}&category={category}&unit={unit}") {
        fun createRoute(houseId: String, config: `in`.xroden.flockr.features.expenses.model.PerDiemConfig) = 
            "edit_per_diem_config/$houseId/${config.id}?itemName=${android.net.Uri.encode(config.itemName)}&rate=${config.rate}&category=${android.net.Uri.encode(config.category)}&unit=${android.net.Uri.encode(config.unit)}"
    }
}
