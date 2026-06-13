package ru.fpvladder.laps.trainer.model.serialization

import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import ru.fpvladder.laps.trainer.model.Counter

object CounterSerializer : KSerializer<Counter> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Counter")

    override fun serialize(encoder: Encoder, value: Counter) {
        when (value) {
            is Counter.Builtin -> encoder.encodeSerializableValue(Counter.Builtin.serializer(), value)
            is Counter.Custom -> encoder.encodeSerializableValue(Counter.Custom.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): Counter {
        val structureDecoder = decoder.beginStructure(descriptor)
        val node = (structureDecoder as YamlInput).node as? YamlMap
            ?: throw SerializationException("Counter must be a YAML map")

        val result = when {
            node.getKey("kind") != null -> structureDecoder.decodeSerializableValue(Counter.Builtin.serializer())
            node.getKey("text") != null -> structureDecoder.decodeSerializableValue(Counter.Custom.serializer())
            else -> throw SerializationException(
                "Cannot determine Counter type: expected 'kind' or 'text' key"
            )
        }

        structureDecoder.endStructure(descriptor)
        return result
    }
}
