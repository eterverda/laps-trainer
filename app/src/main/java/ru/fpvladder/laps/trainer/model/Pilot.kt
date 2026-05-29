package ru.fpvladder.laps.trainer.model

import androidx.compose.ui.graphics.Color

data class Pilot(
    val name: String = "",
    val channelLetter: String = "R",
    val channelNumber: Int = 1,
    val channelColor: ChannelColor = ChannelColor.RED
)

enum class ChannelColor(
    val color: Color,
    val outlineColor: Color? = null
) {
    RED(Color(0xFFFF0000)),
    BLUE(Color(0xFF2979FF)),
    GREEN(Color(0xFF43A047)),
    YELLOW(Color(0xFFFFA000)),
    PURPLE(Color(0xFF800080)),
    FUCHSIA(Color(0xFFFF00FF)),
    CYAN(Color(0xFF00FFFF)),
    WHITE(Color(0xFFFFFFFF), Color(0xFF888888));

    companion object {
        val numbers = (1..8).toList()
    }
}
