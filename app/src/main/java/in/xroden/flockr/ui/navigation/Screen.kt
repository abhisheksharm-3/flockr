package `in`.xroden.flockr.ui.navigation

sealed class Screen(val route: String) {
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
}
