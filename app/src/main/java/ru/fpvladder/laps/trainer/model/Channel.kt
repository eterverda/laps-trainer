package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.model.serialization.ChannelSerializer

@Serializable(ChannelSerializer::class)
data class Channel (
    val letter: String,
    val number: Int,
    val color: Int
) {
    companion object {
        val DEFAULT: Channel = Channel(
            letter = "R",
            number = 1,
            color = 0xFFFF0000.toInt()
        )
    }
}
