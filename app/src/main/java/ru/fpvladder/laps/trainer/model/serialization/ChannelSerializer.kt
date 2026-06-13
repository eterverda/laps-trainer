package ru.fpvladder.laps.trainer.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.fpvladder.laps.trainer.model.Channel

object ChannelSerializer : KSerializer<Channel> {
    override val descriptor = PrimitiveSerialDescriptor(
        "ru.fpvladder.laps.trainer.model.Channel",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Channel) {
        val rgb = value.color and 0x00FFFFFF
        val hex = rgb.toString(16).padStart(6, '0').lowercase()
        encoder.encodeString("${value.letter}${value.number} ${hex}")
    }

    override fun deserialize(decoder: Decoder): Channel {
        val string = decoder.decodeString()
        val match = Regex("""([A-Za-z])(\d+)\s*([0-9A-Fa-f]{6})""")
            .find(string.trim())
            ?: throw SerializationException("Cannot parse Channel from '$string'")

        val (letter, number, hex) = match.destructured
        val rgb = hex.toLong(16).toInt()
        val color = 0xFF000000.toInt() or rgb

        return Channel(
            letter = letter.uppercase(),
            number = number.toInt(),
            color = color
        )
    }
}
