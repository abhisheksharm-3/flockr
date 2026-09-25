package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import kotlin.time.Instant

/**
 * The split rules decide who owes what, so each case here is a real disagreement between
 * housemates if it breaks. Conservation matters most: the payer's share plus every stored row must
 * equal the expense exactly, in every currency and at every group size.
 */
class ExpenseSplitsTest {

    private val cents = 2
    private val yen = 0

    private fun money(value: String) = BigDecimal(value)

    private fun total(shares: SplitShares) = shares.rows.values.fold(shares.payerShare, BigDecimal::add)

    @Test
    fun `even split divides exactly`() {
        val shares = equalShares(money("90.00"), "payer", listOf("a", "b"), cents)
        assertEquals(money("30.00"), shares.payerShare)
        assertEquals(mapOf("a" to money("30.00"), "b" to money("30.00")), shares.rows)
    }

    @Test
    fun `leftover cents go to the members, not the payer`() {
        val shares = equalShares(money("100.00"), "payer", listOf("a", "b"), cents)
        assertEquals(money("33.33"), shares.payerShare)
        assertEquals(money("33.34"), shares.rows.getValue("a"))
        assertEquals(money("33.33"), shares.rows.getValue("b"))
    }

    @Test
    fun `yen splits in whole yen so displayed shares add up`() {
        val shares = equalShares(money("1000"), "payer", listOf("a", "b"), yen)
        assertEquals(money("1000"), total(shares))
        assertTrue(shares.rows.values.all { it.scale() == 0 })
        assertEquals(setOf(money("334"), money("333")), (shares.rows.values + shares.payerShare).toSet())
    }

    @Test
    fun `equal split conserves the total for every amount and group size`() {
        for (units in listOf(1, 2, 5, 7, 35, 99, 100, 333, 1000, 99999)) {
            for (people in 2..12) {
                for (digits in listOf(yen, cents)) {
                    val amount = BigDecimal(units).movePointLeft(digits)
                    val shares = equalShares(amount, "payer", (1 until people).map { "m$it" }, digits)
                    assertEquals("amount=$amount people=$people", amount.setScale(digits), total(shares))
                    val all = shares.rows.values + shares.payerShare
                    assertTrue(all.max() - all.min() <= BigDecimal.ONE.movePointLeft(digits))
                }
            }
        }
    }

    @Test
    fun `a tiny amount split many ways never overshoots the expense`() {
        val shares = equalShares(money("0.35"), "payer", (1..9).map { "m$it" }, cents)
        assertTrue(shares.payerShare.signum() >= 0)
        assertEquals(money("0.35"), total(shares))
    }

    @Test
    fun `payer listed among members is counted once and never gets a row`() {
        val shares = equalShares(money("90.00"), "payer", listOf("payer", "a", "b"), cents)
        assertEquals(equalShares(money("90.00"), "payer", listOf("a", "b"), cents), shares)
        assertTrue("payer" !in shares.rows)
    }

    @Test
    fun `allocation is stable regardless of member order`() {
        assertEquals(
            equalShares(money("100.00"), "payer", listOf("a", "b", "c"), cents),
            equalShares(money("100.00"), "payer", listOf("c", "a", "b"), cents),
        )
    }

    @Test
    fun `custom shares leave the payer the remainder`() {
        val shares = customShares(money("100.00"), "payer", mapOf("a" to money("30.00"), "b" to money("20.00")), cents)!!
        assertEquals(money("50.00"), shares.payerShare)
    }

    @Test
    fun `custom shares reject other members exceeding the expense`() {
        assertNull(customShares(money("100.00"), "payer", mapOf("a" to money("70.00"), "b" to money("40.00")), cents))
    }

    @Test
    fun `a listed payer share must match what the others leave`() {
        val amounts = mapOf("payer" to money("40.00"), "a" to money("50.00"))
        assertNull(customShares(money("100.00"), "payer", amounts, cents))
        assertEquals(money("50.00"), customShares(money("100.00"), "payer", amounts + ("payer" to money("50.00")), cents)!!.payerShare)
    }

    @Test
    fun `custom shares reject more decimals than the currency has`() {
        assertNull(customShares(money("1000"), "payer", mapOf("a" to money("333.5")), yen))
    }

    @Test
    fun `a custom split with a selected member left unpriced is invalid`() {
        val plan = planSplit(money("100.00"), "payer", ExpenseSplitType.AMOUNT, setOf("a", "b"), mapOf("a" to money("30.00")), cents)
        assertNull(plan)
    }

    @Test
    fun `no split leaves the whole expense on the payer`() {
        val plan = planSplit(money("42.00"), "payer", null, emptySet(), null, cents)!!
        assertEquals(money("42.00"), plan.payerShare)
        assertTrue(plan.rows.isEmpty())
    }

    @Test
    fun `percentage splits are refused rather than silently dropped`() {
        assertNull(planSplit(money("42.00"), "payer", ExpenseSplitType.PERCENTAGE, setOf("a"), null, cents))
    }

    @Test
    fun `proportional shares scale a template to this month's amount`() {
        val shares = proportionalShares(money("120.00"), "payer", mapOf("a" to money("60.00"), "b" to money("40.00")), cents)!!
        assertEquals(mapOf("a" to money("72.00"), "b" to money("48.00")), shares.rows)
        assertEquals(money("0.00"), shares.payerShare)
    }

    @Test
    fun `proportional shares conserve awkward totals`() {
        val weights = mapOf("a" to money("1"), "b" to money("1"), "c" to money("1"))
        val shares = proportionalShares(money("100.00"), "a", weights, cents)!!
        assertEquals(money("100.00"), total(shares))
    }

    @Test
    fun `proportional shares refuse negative or all-zero weights`() {
        assertNull(proportionalShares(money("10.00"), "a", mapOf("a" to money("-1")), cents))
        assertNull(proportionalShares(money("10.00"), "a", mapOf("a" to BigDecimal.ZERO), cents))
    }

    private fun bill(
        splitType: ExpenseSplitType?,
        splitWith: List<String>?,
        splitAmounts: Map<String, BigDecimal>? = null,
        amount: String = "100.00",
    ) = RecurringExpense(
        id = "bill",
        houseId = "house",
        name = "Electricity",
        amount = money(amount),
        dueDay = 1,
        category = "Utilities",
        createdBy = "creator",
        createdAt = Instant.fromEpochMilliseconds(0),
        splitWith = splitWith,
        splitType = splitType,
        splitAmounts = splitAmounts,
    )

    @Test
    fun `a custom recurring bill keeps its rows when paid`() {
        val template = bill(ExpenseSplitType.CUSTOM, listOf("a"), mapOf("a" to money("40.00")))
        val shares = recurringPaymentShares(template, money("100.00"), "creator", cents)!!
        assertEquals(mapOf("a" to money("40.00")), shares.rows)
        assertEquals(money("60.00"), shares.payerShare)
    }

    @Test
    fun `a custom recurring bill scales when this month's amount differs`() {
        val template = bill(ExpenseSplitType.CUSTOM, listOf("a"), mapOf("a" to money("40.00")))
        val shares = recurringPaymentShares(template, money("150.00"), "creator", cents)!!
        assertEquals(mapOf("a" to money("60.00")), shares.rows)
        assertEquals(money("90.00"), shares.payerShare)
    }

    @Test
    fun `an equal recurring bill includes its creator when someone else pays`() {
        val template = bill(ExpenseSplitType.EQUAL, listOf("a", "b"), amount = "90.00")
        val shares = recurringPaymentShares(template, money("90.00"), "a", cents)!!
        assertEquals(mapOf("b" to money("30.00"), "creator" to money("30.00")), shares.rows)
        assertEquals(money("30.00"), shares.payerShare)
    }

    @Test
    fun `an unsplit recurring bill leaves it all on the payer`() {
        val shares = recurringPaymentShares(bill(null, null), money("100.00"), "creator", cents)!!
        assertTrue(shares.rows.isEmpty())
        assertEquals(money("100.00"), shares.payerShare)
    }

    @Test
    fun `payload carries the rows as plain decimal strings`() {
        val json = splitRowsJson(mapOf("a" to money("33.34"), "b" to money("33.33"))).toString()
        assertTrue(json.contains("\"33.34\"") && json.contains("\"33.33\""))
    }
}
