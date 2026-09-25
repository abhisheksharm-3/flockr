package `in`.xroden.flockr.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

/**
 * Every amount the user types or reads goes through these functions, so a mistake here shows the
 * wrong figure on every screen at once.
 */
class MoneyTest {

    private lateinit var originalLocale: Locale

    @Before
    fun rememberLocale() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `reads either decimal separator`() {
        assertEquals(BigDecimal("12.50"), parseMoney("12,50", "EUR"))
        assertEquals(BigDecimal("12.50"), parseMoney("12.50", "USD"))
        assertEquals(BigDecimal("1234.56"), parseMoney("1.234,56", "EUR"))
        assertEquals(BigDecimal("1234.56"), parseMoney("1,234.56", "USD"))
    }

    @Test
    fun `rejects what is not a non-negative amount`() {
        listOf("", "  ", "abc", "-5", "12.3.4x").forEach { assertNull("'$it'", parseMoney(it, "USD")) }
    }

    @Test
    fun `rejects more decimals than the currency has instead of rounding`() {
        assertNull(parseMoney("12.345", "USD"))
        assertNull(parseMoney("100.5", "JPY"))
        assertEquals(BigDecimal("100"), parseMoney("100", "JPY"))
    }

    @Test
    fun `quantities accept any precision`() {
        assertEquals(BigDecimal("1.25"), parseDecimal("1,25"))
        assertEquals(BigDecimal("0.125"), parseDecimal("0.125"))
    }

    @Test
    fun `minor units come from the currency`() {
        assertEquals(2, minorUnitDigits("USD"))
        assertEquals(0, minorUnitDigits("JPY"))
        assertEquals(2, minorUnitDigits("NOT-A-CODE"))
    }

    @Test
    fun `formats in the currency's own digits`() {
        Locale.setDefault(Locale.US)
        assertEquals("¥1,000", BigDecimal("1000").formatMoney("JPY"))
        assertEquals("$0.10", BigDecimal("0.1").formatMoney("USD"))
    }

    @Test
    fun `an unknown currency is labelled, not guessed`() {
        assertEquals("XYZ 5.00", BigDecimal("5").formatMoney("XYZ"))
    }

    @Test
    fun `prefill pads to minor units and never rounds a stored amount`() {
        assertEquals("12.50", BigDecimal("12.5").toAmountInput("USD"))
        assertEquals("1000", BigDecimal("1000.00").toAmountInput("JPY"))
        assertEquals("100.5", BigDecimal("100.50").toAmountInput("JPY"))
    }

    @Test
    fun `line totals round half up to the currency`() {
        assertEquals(BigDecimal("5.00"), lineTotal(BigDecimal("1.5"), BigDecimal("3.33"), "USD"))
        assertEquals(BigDecimal("149"), lineTotal(BigDecimal("1.5"), BigDecimal("99"), "JPY"))
    }

    @Test
    fun `apportion always sums to the total and stays within one unit of exact`() {
        val weights = mapOf("a" to BigInteger.ONE, "b" to BigInteger.ONE, "c" to BigInteger.ONE)
        val shares = apportion(BigInteger.valueOf(100), weights, naturalOrder())
        assertEquals(BigInteger.valueOf(100), shares.values.fold(BigInteger.ZERO, BigInteger::add))
        assertEquals(mapOf("a" to 34L, "b" to 33L, "c" to 33L), shares.mapValues { it.value.toLong() })
    }

    @Test
    fun `apportion breaks ties by the given order`() {
        val weights = mapOf("a" to BigInteger.ONE, "b" to BigInteger.ONE)
        val shares = apportion(BigInteger.ONE, weights, compareByDescending { it })
        assertEquals(BigInteger.ONE, shares["b"])
    }

    @Test
    fun `apportion returns nothing when no weight is positive`() {
        assertEquals(emptyMap<String, BigInteger>(), apportion(BigInteger.TEN, mapOf("a" to BigInteger.ZERO), naturalOrder()))
    }
}
