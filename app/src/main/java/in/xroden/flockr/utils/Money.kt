/** Reading and showing amounts and quantities, so every number the app handles follows the same rules. */
package `in`.xroden.flockr.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** The currencies a house can be set to. Symbols and minor units come from [Currency]. */
val SUPPORTED_CURRENCIES: List<String> = listOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "CNY")

private const val FALLBACK_MINOR_DIGITS = 2

private fun currencyOf(code: String): Currency? = runCatching { Currency.getInstance(code) }.getOrNull()

/** How many minor-unit digits [currencyCode] uses: 2 for USD, 0 for JPY. */
fun minorUnitDigits(currencyCode: String): Int =
    currencyOf(currencyCode)?.defaultFractionDigits?.takeIf { it >= 0 } ?: FALLBACK_MINOR_DIGITS

/**
 * Formats this amount in [currencyCode] with the device locale's conventions: its grouping (lakh
 * grouping under en-IN), its symbol placement, and the currency's own minor-unit digits.
 *
 * An unrecognised code is written out as a prefix rather than guessed, so an amount is never
 * shown in the wrong currency.
 */
fun BigDecimal.formatMoney(currencyCode: String): String {
    val currency = currencyOf(currencyCode)
        ?: return "$currencyCode ${setScale(FALLBACK_MINOR_DIGITS, RoundingMode.HALF_UP).toPlainString()}"
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        this.currency = currency
        minimumFractionDigits = minorUnitDigits(currencyCode)
        maximumFractionDigits = minorUnitDigits(currencyCode)
        roundingMode = RoundingMode.HALF_UP
    }.format(this)
}

/** The symbol for [currencyCode] in the device locale, for labelling an amount field. */
fun currencySymbol(currencyCode: String): String =
    currencyOf(currencyCode)?.getSymbol(Locale.getDefault()) ?: currencyCode

/**
 * Reads a non-negative decimal the user typed, whichever decimal separator their keyboard produced.
 *
 * The last `.` or `,` is the decimal separator and any earlier ones are grouping, so "12,50",
 * "12.50", "1.234,56" and "1,234.56" all read as intended. Returns null for anything else.
 */
fun parseDecimal(text: String): BigDecimal? {
    val cleaned = text.trim().filterNot { it == ' ' || it == '\u00A0' || it == '\u202F' }
    if (cleaned.isEmpty()) return null
    val decimalAt = maxOf(cleaned.lastIndexOf('.'), cleaned.lastIndexOf(','))
    val normalized = if (decimalAt < 0) cleaned else {
        cleaned.substring(0, decimalAt).filterNot { it == '.' || it == ',' } + "." + cleaned.substring(decimalAt + 1)
    }
    return normalized.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
}

/**
 * Reads an amount of [currencyCode] the user typed, as [parseDecimal] does. Also returns null for
 * an amount with more decimal places than the currency has minor units, because silently rounding
 * a typed amount changes what the user said they paid.
 */
fun parseMoney(text: String, currencyCode: String): BigDecimal? =
    parseDecimal(text)?.takeIf { it.stripTrailingZeros().scale() <= minorUnitDigits(currencyCode) }

/**
 * This amount as text for prefilling an amount field, padded to [currencyCode]'s minor units so
 * 12.5 reads as "12.50". An amount with more decimals than the currency allows is shown exactly
 * rather than rounded, so reopening a form never changes a stored amount without the user seeing it.
 */
fun BigDecimal.toAmountInput(currencyCode: String): String {
    val digits = minorUnitDigits(currencyCode)
    return if (stripTrailingZeros().scale() <= digits) setScale(digits).toPlainString() else stripTrailingZeros().toPlainString()
}

/**
 * The cost of [quantity] units at [unitPrice], rounded half-up to [currencyCode]'s minor units.
 *
 * A per-diem entry stores this at the moment it is recorded, and a bill is the sum of the stored
 * costs, so every line on a bill adds up to its total and a later price change leaves past months
 * as they were.
 */
fun lineTotal(quantity: BigDecimal, unitPrice: BigDecimal, currencyCode: String): BigDecimal =
    (quantity * unitPrice).setScale(minorUnitDigits(currencyCode), RoundingMode.HALF_UP)
