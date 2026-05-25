package ru.fpvladder.laps.trainer.model

import java.util.UUID

data class Training(
    val id: String = UUID.randomUUID().toString(),
    val type: TrainingType,
    val pilotName: String? = null,
    val channelLetter: String? = null,
    val channelNumber: Int? = null,
    val channelColor: ChannelColor? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TrainingType(val displayName: String) {
    INDIVIDUAL("Индивидуальная"),
    TEAM("Командная")
}
