package ru.fpvladder.laps.trainer.settings

enum class TimerPrecision(
    val displayName: String,
    val description: String,
    val fractionDigits: Int,
    val tickMs: Long
) {
    MILLISECONDS("1 миллисекунда", "Могу уклониться от молнии", 3, 1L),
    CENTISECONDS("1/100 секунды", "Выигрываю у кота в \"царапки\"", 2, 10L),
    DECISECONDS("1/10 секунды", "Успеваю моргнуть глазом", 1, 100L),
    SECONDS("1 секунда", "Наблюдаю миграцию черепах", 0, 1000L);

    fun roundMs(timeMs: Long): Long {
        val factor = tickMs
        return ((timeMs + factor / 2) / factor) * factor
    }
}
