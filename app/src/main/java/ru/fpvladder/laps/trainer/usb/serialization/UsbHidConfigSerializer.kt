package ru.fpvladder.laps.trainer.usb.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import ru.fpvladder.laps.trainer.usb.UsbHidBinding
import ru.fpvladder.laps.trainer.usb.UsbHidConfig
import ru.fpvladder.laps.trainer.usb.UsbHidInfo

/**
 * Serializer for [UsbHidConfig] that preserves the stable [uuid] across saves.
 */
object UsbHidConfigSerializer : KSerializer<UsbHidConfig> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UsbHidConfig") {
        element<String>("uuid")
        element<UsbHidInfo>("info")
        element<List<UsbHidBinding>>("bindings")
    }

    override fun serialize(encoder: Encoder, value: UsbHidConfig) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.uuid)
            encodeSerializableElement(descriptor, 1, UsbHidInfo.serializer(), value.info)
            encodeSerializableElement(
                descriptor,
                2,
                ListSerializer(UsbHidBinding.serializer()),
                value.bindings.toList(),
            )
        }
    }

    override fun deserialize(decoder: Decoder): UsbHidConfig {
        return decoder.decodeStructure(descriptor) {
            var uuid: String? = null
            var info: UsbHidInfo? = null
            var bindings: List<UsbHidBinding>? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> uuid = decodeStringElement(
                        descriptor, index
                    )
                    1 -> info = decodeSerializableElement(
                        descriptor, index, UsbHidInfo.serializer()
                    )
                    2 -> bindings = decodeSerializableElement(
                        descriptor, index, ListSerializer(UsbHidBinding.serializer()),
                    )
                    else -> break
                }
            }
            requireNotNull(uuid) { "Missing 'uuid' in UsbHidConfig" }
            requireNotNull(info) { "Missing 'info' in UsbHidConfig" }
            UsbHidConfig.create(
                uuid = uuid,
                info = info,
                bindings = bindings?.toSet() ?: emptySet(),
            )
        }
    }
}
