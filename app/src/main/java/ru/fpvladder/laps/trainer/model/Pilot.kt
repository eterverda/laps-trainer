package ru.fpvladder.laps.trainer.model

import androidx.compose.ui.graphics.Color

data class Pilot(
    val name: String = "Курсант Задержкин",
    val channelLetter: String = "R",
    val channelNumber: Int = 1,
    val channelColor: ChannelColor = ChannelColor.RED
)

enum class ChannelColor(
    val displayName: String,
    val color: Color,
    val outlineColor: Color? = null
) {
    RED("Красный", Color(0xFFFF0000)),
    BLUE("Синий", Color(0xFF2979FF)),
    GREEN("Зелёный", Color(0xFF43A047)),
    YELLOW("Жёлтый", Color(0xFFFFA000)),
    PURPLE("Пурпурный", Color(0xFF800080)),
    FUCHSIA("Фуксия", Color(0xFFFF00FF)),
    CYAN("Cyan", Color(0xFF00FFFF)),
    WHITE("Белый", Color(0xFFFFFFFF), Color(0xFF888888));

    companion object {
        val letters = listOf("A", "B", "E", "F", "R", "L")
        val numbers = (1..8).toList()
    }
}
