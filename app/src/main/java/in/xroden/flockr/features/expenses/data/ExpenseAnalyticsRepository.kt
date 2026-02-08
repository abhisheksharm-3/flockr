package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.dto.expense.DebtBreakdownItem
import `in`.xroden.flockr.data.dto.expense.GetDebtBreakdownParams
import `in`.xroden.flockr.data.dto.expense.GetMonthlySummaryParams
import `in`.xroden.flockr.data.dto.expense.GetSpendByCategoryParams
import `in`.xroden.flockr.data.dto.expense.GetSpendByMemberParams
import `in`.xroden.flockr.data.dto.expense.GetUserBalancesParams
import `in`.xroden.flockr.features.expenses.model.MonthlySummary
import `in`.xroden.flockr.features.expenses.model.SpendByCategory
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.features.expenses.model.UserBalance
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseAnalyticsRepository @Inject constructor(
    private val supabase: SupabaseClient
) : IExpenseAnalyticsRepository {

    override suspend fun getUserBalances(houseId: String): Result<List<UserBalance>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_user_balances",
            parameters = GetUserBalancesParams(houseId = houseId)
        ).decodeList<UserBalance>()
    }

    override suspend fun getDebtBreakdown(houseId: String, payerId: String, payeeId: String): Result<List<DebtBreakdownItem>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_debt_breakdown",
            parameters = GetDebtBreakdownParams(
                houseId = houseId,
                payerId = payerId,
                payeeId = payeeId
            )
        ).decodeList<DebtBreakdownItem>()
    }

    override suspend fun getMonthlySummary(houseId: String, month: String): Result<MonthlySummary> = runCatching {
        val summaryList = supabase.postgrest.rpc(
            function = "get_monthly_summary",
            parameters = GetMonthlySummaryParams(
                houseId = houseId,
                month = month
            )
        ).decodeList<MonthlySummary>()

        summaryList.firstOrNull() ?: throw IllegalStateException("No summary data returned from RPC")
    }

    override suspend fun getSpendByMember(houseId: String, month: String): Result<List<SpendByMember>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_spend_by_member",
            parameters = GetSpendByMemberParams(
                houseId = houseId,
                month = month
            )
        ).decodeList<SpendByMember>()
    }

    override suspend fun getSpendByCategory(houseId: String, month: String): Result<List<SpendByCategory>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_spend_by_category",
            parameters = GetSpendByCategoryParams(
                houseId = houseId,
                month = month
            )
        ).decodeList<SpendByCategory>()
    }
}
