package `in`.xroden.flockr.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

/**
 * Custom serializer for BigDecimal to preserve precision
 * Serializes as String to prevent floating-point precision loss
 */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val raw = if (decoder is kotlinx.serialization.json.JsonDecoder) {
            (decoder.decodeJsonElement() as? kotlinx.serialization.json.JsonPrimitive)?.content
        } else {
            decoder.decodeString()
        }
        // A malformed/empty/null money value must not crash decoding of the whole row.
        return raw?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }
}


