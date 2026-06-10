package ru.fpvladder.laps.trainer.model

enum class TimerPrecision(val displayName: String, val fractionDigits: Int, val tickMs: Long) {
    SECONDS("1 секунда", 0, 1000L),
    DECISECONDS("1/10 секунды", 1, 100L),
    CENTISECONDS("1/100 секунды", 2, 10L),
    MILLISECONDS("1 миллисекунда", 3, 1L);

    fun roundMs(timeMs: Long): Long {
        val factor = tickMs
        return ((timeMs + factor / 2) / factor) * factor
    }
}
