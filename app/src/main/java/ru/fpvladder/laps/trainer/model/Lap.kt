package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lap(
    val number: Int,
    @SerialName("interval_ms")
    val interval: TimeInterval,
    val success: Boolean,
) {
    val startMs: Long get() = interval.startMs
    val endMs: Long get() = interval.endMs
    val durationMs: Long get() = interval.durationMs
}
