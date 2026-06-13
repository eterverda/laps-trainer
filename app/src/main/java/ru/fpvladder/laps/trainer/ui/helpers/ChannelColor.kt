package ru.fpvladder.laps.trainer.ui.helpers

import androidx.compose.ui.graphics.Color

enum class ChannelColor(
    val color: Int,
    val outlineColor: Int? = null
) {
    RED(0xFFFF0000.toInt()),
    BLUE(0xFF2979FF.toInt()),
    GREEN(0xFF43A047.toInt()),
    YELLOW(0xFFFFA000.toInt()),
    PURPLE(0xFF800080.toInt()),
    FUCHSIA(0xFFFF00FF.toInt()),
    CYAN(0xFF00FFFF.toInt()),
    WHITE(0xFFFFFFFF.toInt(), 0xFF888888.toInt());

    companion object {
        val numbers = (1..8).toList()
    }
}

fun Int.toComposeColor(): Color = Color(this)
