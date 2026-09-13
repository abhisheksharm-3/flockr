package `in`.xroden.flockr.features.expenses.data

import `in`.xroden.flockr.data.enums.ExpenseSplitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * The split allocator decides who owes what, so every case here is a real-world argument between
 * roommates if it breaks. Conservation is the property that matters most: the payer's share plus
 * every stored row must equal the expense exactly, at any amount and any group size.
 */
class ExpenseSplitsTest {

    private fun money(value: String) = BigDecimal(value)

    private fun allocatedTotal(shares: EqualShares) =
        shares.rows.values.fold(shares.payerShare, BigDecimal::add)

    @Test
    fun `even split divides exactly`() {
        val shares = equalShares(money("90.00"), "payer", listOf("a", "b"))
        assertEquals(money("30.00"), shares.payerShare)
        assertEquals(mapOf("a" to money("30.00"), "b" to money("30.00")), shares.rows)
    }

    @Test
    fun `uneven split conserves the total instead of dropping a cent`() {
        val shares = equalShares(money("100.00"), "payer", listOf("a", "b"))
        assertEquals(money("100.00"), allocatedTotal(shares))
    }

    @Test
    fun `leftover cents go to the members, not the payer`() {
        val shares = equalShares(money("100.00"), "payer", listOf("a", "b"))
        assertEquals(money("33.33"), shares.payerShare)
        assertEquals(money("33.34"), shares.rows.getValue("a"))
        assertEquals(money("33.33"), shares.rows.getValue("b"))
    }

    @Test
    fun `no two shares differ by more than a cent`() {
        for (participants in 2..25) {
            val others = (1 until participants).map { "member$it" }
            val shares = equalShares(money("100.00"), "payer", others)
            val all = shares.rows.values + shares.payerShare
            val spread = all.max().subtract(all.min())
            assertTrue(
                "spread of ${spread.toPlainString()} across $participants participants",
                spread <= money("0.01")
            )
        }
    }

    @Test
    fun `conserves the total across many amounts and group sizes`() {
        for (cents in listOf(1, 2, 5, 7, 35, 99, 100, 333, 1000, 99999)) {
            for (participants in 2..12) {
                val amount = BigDecimal(cents).movePointLeft(2)
                val others = (1 until participants).map { "member$it" }
                val shares = equalShares(amount, "payer", others)
                assertEquals(
                    "amount=${amount.toPlainString()} participants=$participants",
                    amount.setScale(2),
                    allocatedTotal(shares)
                )
            }
        }
    }

    @Test
    fun `a tiny amount split many ways never overshoots the expense`() {
        val others = (1..9).map { "member$it" }
        val shares = equalShares(money("0.35"), "payer", others)
        val owed = shares.rows.values.fold(BigDecimal.ZERO, BigDecimal::add)
        assertTrue("rows totalled ${owed.toPlainString()}", owed <= money("0.35"))
        assertTrue("payer share was negative", shares.payerShare >= BigDecimal.ZERO)
        assertEquals(money("0.35"), allocatedTotal(shares))
    }

    @Test
    fun `payer listed among the members is counted once`() {
        val withPayer = equalShares(money("90.00"), "payer", listOf("payer", "a", "b"))
        val withoutPayer = equalShares(money("90.00"), "payer", listOf("a", "b"))
        assertEquals(withoutPayer.rows, withPayer.rows)
        assertEquals(withoutPayer.payerShare, withPayer.payerShare)
    }

    @Test
    fun `payer never receives a split row`() {
        val shares = equalShares(money("100.00"), "payer", listOf("payer", "a"))
        assertTrue(shares.rows.keys.none { it == "payer" })
    }

    @Test
    fun `nobody to split with leaves the whole expense on the payer`() {
        val shares = equalShares(money("42.00"), "payer", emptyList())
        assertEquals(money("42.00"), shares.payerShare)
        assertTrue(shares.rows.isEmpty())
    }

    @Test
    fun `a non-positive amount produces no rows`() {
        listOf("0.00", "-5.00").forEach { value ->
            val shares = equalShares(money(value), "payer", listOf("a", "b"))
            assertTrue("$value produced rows", shares.rows.isEmpty())
            assertTrue("$value gave the payer a negative share", shares.payerShare >= BigDecimal.ZERO)
        }
    }

    @Test
    fun `allocation is stable regardless of member order`() {
        val one = equalShares(money("100.00"), "payer", listOf("a", "b", "c"))
        val other = equalShares(money("100.00"), "payer", listOf("c", "a", "b"))
        assertEquals(one.rows, other.rows)
    }

    @Test
    fun `equal payload carries the allocated rows and omits the payer`() {
        val json = buildExpenseSplitsJson(
            amount = money("100.00"),
            payerId = "payer",
            splitWith = listOf("a", "b"),
            splitType = ExpenseSplitType.EQUAL,
            splitAmounts = null
        )
        assertEquals(2, json.size)
        val encoded = json.toString()
        assertTrue(encoded.contains("33.34"))
        assertTrue(encoded.contains("33.33"))
        assertTrue(!encoded.contains("\"payer\""))
    }

    @Test
    fun `custom payload drops a row for the payer`() {
        val json = buildExpenseSplitsJson(
            amount = money("100.00"),
            payerId = "payer",
            splitWith = listOf("payer", "a"),
            splitType = ExpenseSplitType.AMOUNT,
            splitAmounts = mapOf("payer" to money("50.00"), "a" to money("50.00"))
        )
        assertEquals(1, json.size)
        assertTrue(json.toString().contains("\"a\""))
    }
}
