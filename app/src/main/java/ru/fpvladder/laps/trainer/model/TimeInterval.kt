package ru.fpvladder.laps.trainer.model

data class TimeInterval(
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = endMs - startMs
}
