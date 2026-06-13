package ru.fpvladder.laps.trainer.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MinMaxIntSerializer : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor(
        "ru.fpvladder.laps.trainer.model.MinMaxInt",
        PrimitiveKind.INT
    )

    override fun serialize(encoder: Encoder, value: Int) {
        when (value) {
            Int.MIN_VALUE -> encoder.encodeString("min")
            Int.MAX_VALUE -> encoder.encodeString("max")
            else -> encoder.encodeInt(value)
        }
    }

    override fun deserialize(decoder: Decoder): Int {
        return try {
            decoder.decodeInt()
        } catch (_: Exception) {
            val string = decoder.decodeString()
            when (string) {
                "min" -> Int.MIN_VALUE
                "max" -> Int.MAX_VALUE
                else -> string.toIntOrNull()
                    ?: throw SerializationException("Cannot parse min/max int from '$string'")
            }
        }
    }
}
