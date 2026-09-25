/** Dividing a whole number of units in proportion to weights without losing or inventing any. */
package `in`.xroden.flockr.utils

import java.math.BigInteger

/**
 * Splits [total] whole units across [weights] by the largest-remainder method.
 *
 * Each key first gets the floor of its exact share. The units that leaves over go one each to the
 * keys with the largest remainders, ties broken by [tieOrder]. The results always sum to [total]
 * and each is within one unit of its exact share, which is what lets split rows add up to the
 * expense to the cent and chart percentages add up to 100.
 *
 * Returns an empty map when no weight is positive.
 *
 * @throws IllegalArgumentException when a weight is negative.
 */
fun <K> apportion(total: BigInteger, weights: Map<K, BigInteger>, tieOrder: Comparator<K>): Map<K, BigInteger> {
    require(weights.values.all { it.signum() >= 0 }) { "Weights must not be negative" }
    val weightTotal = weights.values.fold(BigInteger.ZERO, BigInteger::add)
    if (weightTotal.signum() <= 0) return emptyMap()

    val exact = weights.mapValues { (_, weight) -> (total * weight).divideAndRemainder(weightTotal) }
    val leftover = (total - exact.values.fold(BigInteger.ZERO) { acc, qr -> acc + qr[0] }).toInt()
    val bonusRecipients = exact.keys
        .sortedWith(compareByDescending<K> { exact.getValue(it)[1] }.then(tieOrder))
        .take(leftover)
        .toSet()

    return exact.mapValues { (key, qr) -> if (key in bonusRecipients) qr[0] + BigInteger.ONE else qr[0] }
}
