package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseDueStatus
import `in`.xroden.flockr.data.enums.SplitMethod
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.RecurringShare
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Every split must add up to the expense to the smallest unit, or balances drift apart. */
class ExpenseSplitsTest {

    private fun dec(value: String) = BigDecimal(value)

    private fun shares(
        amount: String,
        method: SplitMethod?,
        values: Map<String, String>,
        payer: String = "a",
        digits: Int = 2,
    ): List<ExpenseShare>? = expenseShares(dec(amount), payer, method, values.mapValues { dec(it.value) }, digits)

    private fun List<ExpenseShare>.owed() = associate { it.userId to it.owedShare.toPlainString() }

    private fun List<ExpenseShare>.assertBalanced(amount: String) {
        assertEquals(0, dec(amount).compareTo(sumOf { it.paidShare }))
        assertEquals(0, dec(amount).compareTo(sumOf { it.owedShare }))
    }

    @Test
    fun `equal split divides exactly`() {
        val rows = shares("90", SplitMethod.EQUAL, mapOf("a" to "1", "b" to "1", "c" to "1"))!!
        assertEquals(mapOf("a" to "30.00", "b" to "30.00", "c" to "30.00"), rows.owed())
        assertEquals("90.00", rows.single { it.userId == "a" }.paidShare.toPlainString())
    }

    @Test
    fun `leftover cents go to the others before the payer`() {
        val rows = shares("100", SplitMethod.EQUAL, mapOf("a" to "1", "b" to "1", "c" to "1"))!!
        assertEquals(mapOf("a" to "33.33", "b" to "33.34", "c" to "33.33"), rows.owed())
        rows.assertBalanced("100")
    }

    @Test
    fun `yen splits in whole yen`() {
        val rows = shares("1000", SplitMethod.EQUAL, mapOf("a" to "1", "b" to "1", "c" to "1"), digits = 0)!!
        assertEquals(mapOf("a" to "333", "b" to "334", "c" to "333"), rows.owed())
    }

    @Test
    fun `every equal split conserves the total`() {
        for (cents in 1..500) for (people in 1..7) {
            val amount = BigDecimal.valueOf(cents.toLong(), 2).toPlainString()
            val rows = shares(amount, SplitMethod.EQUAL, (1..people).associate { "m$it" to "1" }, payer = "m1")!!
            rows.assertBalanced(amount)
        }
    }

    @Test
    fun `the payer can pay for something only the others share`() {
        val rows = shares("50", SplitMethod.EQUAL, mapOf("b" to "1", "c" to "1"))!!
        assertEquals(mapOf("b" to "25.00", "c" to "25.00", "a" to "0.00"), rows.owed())
        assertEquals("50.00", rows.single { it.userId == "a" }.paidShare.toPlainString())
    }

    @Test
    fun `shares weight the split`() {
        val rows = shares("90", SplitMethod.SHARES, mapOf("a" to "2", "b" to "1"))!!
        assertEquals(mapOf("a" to "60.00", "b" to "30.00"), rows.owed())
        assertEquals("2", rows.single { it.userId == "a" }.splitValue!!.toPlainString())
    }

    @Test
    fun `percentages must total a hundred`() {
        assertEquals(
            mapOf("a" to "33.34", "b" to "66.66"),
            shares("100", SplitMethod.PERCENT, mapOf("a" to "33.34", "b" to "66.66"))!!.owed(),
        )
        assertNull(shares("100", SplitMethod.PERCENT, mapOf("a" to "40", "b" to "50")))
    }

    @Test
    fun `awkward percentages still conserve the total`() {
        shares("10", SplitMethod.PERCENT, mapOf("a" to "33.3333", "b" to "33.3333", "c" to "33.3334"))!!.assertBalanced("10")
    }

    @Test
    fun `exact amounts must total the expense in whole units`() {
        assertEquals(mapOf("a" to "70.00", "b" to "30.00"), shares("100", SplitMethod.EXACT, mapOf("a" to "70", "b" to "30"))!!.owed())
        assertNull(shares("100", SplitMethod.EXACT, mapOf("a" to "70", "b" to "20")))
        assertNull(shares("100", SplitMethod.EXACT, mapOf("a" to "70.005", "b" to "29.995")))
    }

    @Test
    fun `no split leaves the whole expense on the payer`() {
        val rows = shares("42.50", null, emptyMap())!!
        assertEquals(listOf(ExpenseShare("a", dec("42.50"), dec("42.50"))), rows)
    }

    @Test
    fun `invalid input is refused rather than rounded`() {
        assertNull(shares("10.005", SplitMethod.EQUAL, mapOf("a" to "1", "b" to "1")))
        assertNull(shares("0", SplitMethod.EQUAL, mapOf("a" to "1", "b" to "1")))
        assertNull(shares("10", SplitMethod.SHARES, mapOf("a" to "0", "b" to "1")))
        assertNull(shares("10", SplitMethod.EQUAL, emptyMap()))
    }

    @Test
    fun `a tiny amount leaves out whoever owes nothing`() {
        val rows = shares("0.01", SplitMethod.EQUAL, mapOf("a" to "1", "b" to "1", "c" to "1"), payer = "a")!!
        rows.assertBalanced("0.01")
        assertEquals(setOf("a", "b"), rows.map { it.userId }.toSet())
    }

    private fun bill(amount: String, method: SplitMethod?, values: Map<String, String>) = RecurringExpense(
        id = "r", name = "Rent", amount = dec(amount), category = "Rent",
        firstDueDate = LocalDate(2026, 1, 1), nextDueDate = LocalDate(2026, 1, 1),
        splitMethod = method, createdBy = "a", shares = values.map { RecurringShare(it.key, dec(it.value)) },
        dueStatus = ExpenseDueStatus.SCHEDULED, daysUntilDue = 0,
    )

    @Test
    fun `an exact bill keeps its amounts when paid in full`() {
        val rows = recurringPaymentShares(bill("100", SplitMethod.EXACT, mapOf("a" to "60", "b" to "40")), dec("100"), "b", 2)!!
        assertEquals(mapOf("a" to "60.00", "b" to "40.00"), rows.owed())
        assertEquals("100.00", rows.single { it.userId == "b" }.paidShare.toPlainString())
    }

    @Test
    fun `an exact bill scales when this payment differs`() {
        val rows = recurringPaymentShares(bill("100", SplitMethod.EXACT, mapOf("a" to "60", "b" to "40")), dec("120"), "b", 2)!!
        assertEquals(mapOf("a" to "72.00", "b" to "48.00"), rows.owed())
    }

    @Test
    fun `an unsplit bill leaves it all on whoever pays`() {
        val rows = recurringPaymentShares(bill("80", null, emptyMap()), dec("80"), "b", 2)!!
        assertEquals(mapOf("b" to "80.00"), rows.owed())
    }

    @Test
    fun `payload carries amounts as plain decimal strings`() {
        val json = sharesJson(listOf(ExpenseShare("a", dec("1E+2"), dec("0.10"), dec("2")))).toString()
        assertEquals("""[{"user_id":"a","paid_share":"100","owed_share":"0.10","split_value":"2"}]""", json)
    }
}
