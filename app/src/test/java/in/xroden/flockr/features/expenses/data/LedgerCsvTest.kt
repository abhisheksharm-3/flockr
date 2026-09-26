package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.ExpenseKind
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import java.math.BigDecimal
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerCsvTest {

    private val alex = MemberWithProfile(userId = "a", fullName = "Alex", email = "a@x.test", joinedAt = Instant.fromEpochSeconds(0))
    private val riya = MemberWithProfile(userId = "r", fullName = "Riya", email = "r@x.test", joinedAt = Instant.fromEpochSeconds(0))

    private fun expense(id: String, name: String, amount: String, day: Int, kind: ExpenseKind, shares: List<ExpenseShare>, notes: String? = null) =
        Expense(
            id = id, houseId = "h", kind = kind, name = name, amount = BigDecimal(amount),
            category = if (kind == ExpenseKind.EXPENSE) "Groceries" else null,
            date = LocalDate(2026, 9, day), notes = notes, createdBy = "a",
            createdAt = Instant.fromEpochSeconds(day.toLong()), shares = shares,
        )

    @Test
    fun `rows run oldest first with each person's balance change, payments named, text made safe`() {
        val groceries = expense(
            "e1", "Milk, eggs", "100.50", 20, ExpenseKind.EXPENSE,
            listOf(ExpenseShare("a", BigDecimal("100.50"), BigDecimal("50.25")), ExpenseShare("r", BigDecimal.ZERO, BigDecimal("50.25"))),
            notes = "=cmd()",
        )
        val payback = expense(
            "e2", "Payment", "50.25", 21, ExpenseKind.SETTLEMENT,
            listOf(ExpenseShare("r", BigDecimal("50.25"), BigDecimal.ZERO), ExpenseShare("a", BigDecimal.ZERO, BigDecimal("50.25"))),
        )

        assertEquals(
            "Date,Type,Description,Category,Amount,Currency,Notes,Alex,Riya\r\n" +
                "2026-09-20,Expense,\"Milk, eggs\",Groceries,100.50,INR,'=cmd(),50.25,-50.25\r\n" +
                "2026-09-21,Payment,Riya paid Alex,,50.25,INR,,-50.25,50.25\r\n",
            ledgerCsv(listOf(payback, groceries), listOf(alex, riya), "INR"),
        )
    }
}
