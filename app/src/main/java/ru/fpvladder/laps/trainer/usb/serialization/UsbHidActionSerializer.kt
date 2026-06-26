package ru.fpvladder.laps.trainer.usb.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.fpvladder.laps.trainer.usb.UsbHidAction

object UsbHidActionSerializer : KSerializer<UsbHidAction> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UsbHidAction", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UsbHidAction) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): UsbHidAction {
        return UsbHidAction.valueOf(decoder.decodeString().uppercase())
    }
}
