package ru.fpvladder.laps.trainer.ui.helpers

import androidx.compose.ui.graphics.Color

enum class ChannelColor(
    val color: Int,
    val lightOutline: Int? = null,
    val darkOutline: Int? = null
) {
    RED(0xFFFF0000.toInt()),
    BLUE(0xFF2979FF.toInt()),
    GREEN(0xFF43A047.toInt()),
    YELLOW(0xFFFFD54F.toInt()),
    FUCHSIA(0xFFFF00FF.toInt()),
    CYAN(0xFF00FFFF.toInt()),
    WHITE(0xFFFFFFFF.toInt(), lightOutline = LIGHT_OUTLINE),
    BLACK(0xFF000000.toInt(), darkOutline = DARK_OUTLINE);

    companion object {
        val numbers = (1..8).toList()
    }
}

fun Int.toComposeColor(): Color = Color(this)

const val LIGHT_OUTLINE = 0xFF6c7086.toInt()
const val DARK_OUTLINE = 0xFF9399b2.toInt()
