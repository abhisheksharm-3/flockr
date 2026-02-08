package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.PaymentHistoryInsert
import `in`.xroden.flockr.data.dto.RecurringExpenseInsert
import `in`.xroden.flockr.data.dto.RecurringExpenseUpdate
import `in`.xroden.flockr.data.dto.expense.GetRecurringExpensesParams
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.model.PaymentHistory
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager,
    private val expenseRepository: ExpenseRepository
) : BaseRealtimeRepository(supabase, connectionManager), IRecurringExpenseRepository {

    override fun getRecurringExpensesFlow(houseId: String): Flow<Result<List<RecurringExpense>>> =
        createRealtimeFlow(
            channelId = "recurring_expenses_$houseId",
            table = "recurring_expenses",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getRecurringExpenses(houseId) }
        )

    override suspend fun getRecurringExpenses(houseId: String): Result<List<RecurringExpense>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_recurring_expenses_with_status",
            parameters = GetRecurringExpensesParams(houseId = houseId)
        ).decodeList<RecurringExpense>()
    }

    override suspend fun createRecurringExpense(
        houseId: String,
        name: String,
        amount: BigDecimal,
        dueDay: Int,
        category: String,
        frequency: `in`.xroden.flockr.data.enums.ExpenseFrequency,
        customFrequencyDays: Int?,
        reminderDaysBefore: Int,
        reminderEnabled: Boolean,
        notes: String?,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?,
        prepayEnabled: Boolean,
        firstPaymentDate: LocalDate?
    ): Result<RecurringExpense> = runCatching {
        val currentUserId = requireAuthenticated(authenticatedUserId)

        supabase.from("recurring_expenses")
            .insert(
                RecurringExpenseInsert(
                    houseId = houseId,
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    category = category,
                    createdBy = currentUserId,
                    frequency = frequency,
                    customFrequencyDays = customFrequencyDays,
                    reminderDaysBefore = reminderDaysBefore,
                    reminderEnabled = reminderEnabled,
                    notes = notes,
                    splitWith = splitWith,
                    splitType = splitType,
                    splitAmounts = splitAmounts,
                    prepayEnabled = prepayEnabled,
                    firstPaymentDate = firstPaymentDate
                )
            ) { select() }
            .decodeSingle<RecurringExpense>()
    }

    override suspend fun updateRecurringExpense(
        expenseId: String,
        name: String?,
        amount: BigDecimal?,
        dueDay: Int?,
        category: String?,
        isActive: Boolean?,
        frequency: `in`.xroden.flockr.data.enums.ExpenseFrequency?,
        lastPaidDate: LocalDate?,
        customFrequencyDays: Int?,
        reminderDaysBefore: Int?,
        reminderEnabled: Boolean?,
        notes: String?,
        splitWith: List<String>?,
        splitType: ExpenseSplitType?,
        splitAmounts: Map<String, BigDecimal>?
    ): Result<Unit> = runCatching {
        supabase.from("recurring_expenses")
            .update(
                RecurringExpenseUpdate(
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    category = category,
                    isActive = isActive,
                    frequency = frequency,
                    lastPaidDate = lastPaidDate,
                    customFrequencyDays = customFrequencyDays,
                    reminderDaysBefore = reminderDaysBefore,
                    reminderEnabled = reminderEnabled,
                    notes = notes,
                    splitWith = splitWith,
                    splitType = splitType,
                    splitAmounts = splitAmounts
                )
            ) {
                filter { eq("id", expenseId) }
            }
    }

    override suspend fun deleteRecurringExpense(expenseId: String): Result<Unit> = runCatching {
        supabase.from("payment_history").delete { filter { eq("recurring_expense_id", expenseId) } }
        supabase.from("recurring_expenses").delete { filter { eq("id", expenseId) } }
    }

    override suspend fun markRecurringExpenseAsPaid(
        expenseId: String,
        amount: BigDecimal,
        paymentDate: LocalDate
    ): Result<Unit> = runCatching {
        val currentUserId = requireAuthenticated(authenticatedUserId)

        val recurringExpense = supabase.from("recurring_expenses")
            .select(Columns.list("*, split_with, split_type, split_amounts")) {
                filter { eq("id", expenseId) }
            }
            .decodeSingleOrNull<RecurringExpense>()
            ?: throw IllegalStateException("Recurring expense not found")

        supabase.from("payment_history")
            .insert(PaymentHistoryInsert(
                recurringExpenseId = expenseId,
                paidBy = currentUserId,
                amount = amount,
                paymentDate = paymentDate
            ))

        expenseRepository.createOneTimeExpense(
            houseId = recurringExpense.houseId,
            name = recurringExpense.name,
            amount = amount,
            category = recurringExpense.category,
            paidBy = currentUserId,
            date = paymentDate,
            notes = "Recurring Payment",
            splitWith = recurringExpense.splitWith,
            splitType = recurringExpense.splitType,
            customAmounts = recurringExpense.splitAmounts
        ).getOrThrow()

        supabase.from("recurring_expenses")
            .update(RecurringExpenseUpdate(lastPaidDate = paymentDate)) {
                filter { eq("id", expenseId) }
            }
    }

    override suspend fun getPaymentHistory(recurringExpenseId: String): Result<List<PaymentHistory>> = runCatching {
        supabase.from("payment_history")
            .select(Columns.ALL) {
                filter { eq("recurring_expense_id", recurringExpenseId) }
                order("payment_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<PaymentHistory>()
    }
}
