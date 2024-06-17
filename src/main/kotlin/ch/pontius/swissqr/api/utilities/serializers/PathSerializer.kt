package ch.pontius.swissqr.api.utilities.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A [KSerializer] for [Path] objects.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("path", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Path = Paths.get(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }
}