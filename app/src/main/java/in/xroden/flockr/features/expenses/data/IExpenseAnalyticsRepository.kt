package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.dto.expense.DebtBreakdownItem
import `in`.xroden.flockr.features.expenses.model.MonthlySummary
import `in`.xroden.flockr.features.expenses.model.SpendByCategory
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.features.expenses.model.UserBalance

/**
 * Interface for expense analytics operations.
 * Provides aggregated expense data for dashboards and reports.
 */
interface IExpenseAnalyticsRepository {
    
    /**
     * Gets the current balance for each member in a house.
     * @param houseId The house to get balances for.
     * @return List of user balances showing who owes whom.
     */
    suspend fun getUserBalances(houseId: String): Result<List<UserBalance>>
    
    /**
     * Gets a breakdown of individual expenses between two members.
     * @param houseId The house context.
     * @param payerId The member who paid.
     * @param payeeId The member who owes.
     * @return List of individual transactions comprising the debt.
     */
    suspend fun getDebtBreakdown(houseId: String, payerId: String, payeeId: String): Result<List<DebtBreakdownItem>>
    
    /**
     * Gets a summary of expenses for a specific month.
     * @param houseId The house to analyze.
     * @param month The month in format "YYYY-MM-DD" (first day of month).
     * @return Aggregated monthly summary data.
     */
    suspend fun getMonthlySummary(houseId: String, month: String): Result<MonthlySummary>
    
    /**
     * Gets spending totals grouped by house member.
     * @param houseId The house to analyze.
     * @param month The month in format "YYYY-MM-DD".
     * @return List of spending amounts per member.
     */
    suspend fun getSpendByMember(houseId: String, month: String): Result<List<SpendByMember>>
    
    /**
     * Gets spending totals grouped by category.
     * @param houseId The house to analyze.
     * @param month The month in format "YYYY-MM-DD".
     * @return List of spending amounts per category.
     */
    suspend fun getSpendByCategory(houseId: String, month: String): Result<List<SpendByCategory>>
}
