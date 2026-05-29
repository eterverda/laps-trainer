package ru.fpvladder.laps.trainer.model

enum class ChannelGrid(val displayName: String) {
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
        ChannelGrid.HDZERO -> listOf("R", "F", "E")
        ChannelGrid.HDZERO_LOWBAND -> listOf("R", "F", "E", "L")
        ChannelGrid.ANALOG -> listOf("R", "L", "A", "B", "E", "F")
    }

    fun availableNumbers(grid: ChannelGrid, letter: String): List<Int> = when (grid) {
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

    fun isValidColor(colorCount: ColorCount, color: ChannelColor): Boolean {
        return color in availableColors(colorCount)
    }
}
