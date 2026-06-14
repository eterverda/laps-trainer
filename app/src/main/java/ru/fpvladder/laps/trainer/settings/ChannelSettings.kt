package ru.fpvladder.laps.trainer.settings

import ru.fpvladder.laps.trainer.ui.helpers.ChannelColor

enum class ChannelGrid(val displayName: String) {
    RACEBAND("Raceband"),
    HDZERO("HDZero"),
    HDZERO_LOWBAND("HDZero + Lowband"),
    ANALOG("Analog")
}

enum class ColorCount(val displayName: String, val count: Int) {
    FOUR("4 цвета", 4),
    EIGHT("8 цветов", 8)
}

object ChannelConfig {

    fun availableLetters(grid: ChannelGrid): List<String> = when (grid) {
        ChannelGrid.RACEBAND -> listOf("R")
        ChannelGrid.HDZERO -> listOf("R", "F", "E")
        ChannelGrid.HDZERO_LOWBAND -> listOf("R", "F", "E", "L")
        ChannelGrid.ANALOG -> listOf("R", "L", "A", "B", "E", "F")
    }

    fun availableNumbers(grid: ChannelGrid, letter: String): List<Int> = when (grid) {
        ChannelGrid.RACEBAND -> (1..8).toList()
        ChannelGrid.HDZERO -> when (letter) {
            "R" -> (1..8).toList()
            "F" -> listOf(1, 2, 4)
            "E" -> listOf(1)
            else -> emptyList()
        }
        ChannelGrid.HDZERO_LOWBAND -> when (letter) {
            "R" -> (1..8).toList()
            "L" -> (1..8).toList()
            "F" -> listOf(1, 2, 4)
            "E" -> listOf(1)
            else -> emptyList()
        }
        ChannelGrid.ANALOG -> (1..8).toList()
    }

    fun availableColors(colorCount: ColorCount): List<ChannelColor> =
        ChannelColor.entries.take(colorCount.count)

    fun isValidChannel(grid: ChannelGrid, letter: String, number: Int): Boolean {
        val letters = availableLetters(grid)
        if (letter !in letters) return false
        return number in availableNumbers(grid, letter)
    }

}
