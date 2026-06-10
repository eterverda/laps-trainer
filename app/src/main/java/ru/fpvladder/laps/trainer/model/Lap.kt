package ru.fpvladder.laps.trainer.model

data class Lap(
    val label: String,
    val timeMs: Long,
    val status: Status = Status.SUCCESS
) {
    enum class Status {
        HS,
        FAIL,
        SUCCESS
    }
}
