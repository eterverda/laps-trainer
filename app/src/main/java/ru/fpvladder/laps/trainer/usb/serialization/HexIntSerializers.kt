package ru.fpvladder.laps.trainer.usb.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private fun Encoder.encodeHexInt(
    value: Int,
    upperCase: Boolean = true,
    minDigits: Int = 8,
) {
    val hex = value.toString(16)
        .let { if (upperCase) it.uppercase() else it.lowercase() }
        .padStart(minDigits, '0')
    encodeString("0x$hex")
}

private fun Decoder.decodeHexInt(): Int {
    val string = decodeString().trim()
    return when {
        string.startsWith("0x", ignoreCase = true) -> string.drop(2).toInt(16)
        else -> string.toInt(10)
    }
}

internal object IntegerAsUpperHexShortSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntegerAsUpperHexShortSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeHexInt(value, upperCase = true, minDigits = 4)
    }

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeHexInt()
    }
}

internal object IntegerAsLowerHexByteSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntegerAsLowerHexByteSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeHexInt(value, false, 2)
    }

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeHexInt()
    }
}
