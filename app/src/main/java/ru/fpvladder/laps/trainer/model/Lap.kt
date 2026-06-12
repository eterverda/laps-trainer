package ru.fpvladder.laps.trainer.model

data class Lap(
    val label: String,
    val interval: TimeInterval,
    val status: Status = Status.SUCCESS
) {
    enum class Status {
        HS,
        FAIL,
        SUCCESS
    }

    val startMs: Long get() = interval.startMs
    val endMs: Long get() = interval.endMs
    val timeMs: Long get() = interval.durationMs
}
