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
    
    // Modern Finance Screens
    object OneTimeExpenses : Screen("one_time_expenses/{houseId}") {
        fun createRoute(houseId: String) = "one_time_expenses/$houseId"
    }
    object RecurringExpenses : Screen("recurring_expenses/{houseId}") {
        fun createRoute(houseId: String) = "recurring_expenses/$houseId"
    }
    object AddRecurringExpense : Screen("add_recurring_expense/{houseId}") {
        fun createRoute(houseId: String) = "add_recurring_expense/$houseId"
    }
    object MonthlyReports : Screen("monthly_reports/{houseId}") {
        fun createRoute(houseId: String) = "monthly_reports/$houseId"
    }
    object AddExpenseModern : Screen("add_expense_modern/{houseId}?itemName={itemName}&quantity={quantity}") {
        fun createRoute(houseId: String, itemName: String? = null, quantity: Int? = null): String {
            var route = "add_expense_modern/$houseId"
            if (itemName != null) {
                route += "?itemName=${java.net.URLEncoder.encode(itemName, "UTF-8")}"
                if (quantity != null) {
                    route += "&quantity=$quantity"
                }
            }
            return route
        }
    }
    object BalancesModern : Screen("balances_modern/{houseId}") {
        fun createRoute(houseId: String) = "balances_modern/$houseId"
    }
    object QuickPerDiemEntry : Screen("quick_per_diem/{houseId}") {
        fun createRoute(houseId: String) = "quick_per_diem/$houseId"
    }
    object PerDiemTransactions : Screen("per_diem_transactions/{houseId}") {
        fun createRoute(houseId: String) = "per_diem_transactions/$houseId"
    }

    // Modern Organization Screens
    object ShoppingListModern : Screen("shopping_modern/{houseId}") {
        fun createRoute(houseId: String) = "shopping_modern/$houseId"
    }
    object ChoresModern : Screen("chores_modern/{houseId}") {
        fun createRoute(houseId: String) = "chores_modern/$houseId"
    }
}
