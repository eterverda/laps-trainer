package ru.fpvladder.laps.trainer.model

data class Channel(
    val letter: String = "R",
    val number: Int = 1,
    val color: Int = 0xFFFF0000.toInt()
)
