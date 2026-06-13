package ru.fpvladder.laps.trainer.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.fpvladder.laps.trainer.model.TimeInterval

object TimeIntervalSerializer : KSerializer<TimeInterval> {
    override val descriptor = PrimitiveSerialDescriptor(
        "ru.fpvladder.laps.trainer.model.TimeInterval",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: TimeInterval) {
        encoder.encodeString("${value.startMs} -> ${value.endMs}")
    }

    override fun deserialize(decoder: Decoder): TimeInterval {
        val string = decoder.decodeString()
        val match = Regex("""(\d+).*?(\d+)""")
            .find(string)
            ?: throw SerializationException("Cannot parse TimeInterval from '$string'")

        val (start, end) = match.destructured
        return TimeInterval(startMs = start.toLong(), endMs = end.toLong())
    }
}
