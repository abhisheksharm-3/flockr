/** Carries money and quantities as exact decimals, never through a floating-point number. */
package `in`.xroden.flockr.data.serialization

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * Writes a plain decimal string and reads either a JSON number or string, from its text, so no
 * digit is lost. A value that is not a number fails the read rather than turning into zero, which
 * would show a wrong amount as if it were right.
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())

    override fun deserialize(decoder: Decoder): BigDecimal {
        val raw = if (decoder is JsonDecoder) (decoder.decodeJsonElement() as? JsonPrimitive)?.content else decoder.decodeString()
        return raw?.toBigDecimalOrNull() ?: throw SerializationException("Not a decimal: $raw")
    }
}
