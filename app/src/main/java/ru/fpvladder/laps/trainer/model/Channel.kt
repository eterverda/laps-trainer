package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.model.serialization.ChannelSerializer

@Serializable(ChannelSerializer::class)
data class Channel(
    val letter: String = "R",
    val number: Int = 1,
    val color: Int = 0xFFFF0000.toInt()
)
