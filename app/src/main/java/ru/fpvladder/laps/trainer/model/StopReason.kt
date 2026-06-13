package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.Serializable

@Serializable
enum class StopReason {
    TIME_LIMIT,
    LAPS_LIMIT,
    MANUAL
}
