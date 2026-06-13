package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.model.serialization.TimeIntervalSerializer

@Serializable(TimeIntervalSerializer::class)
data class TimeInterval(
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = endMs - startMs
}
