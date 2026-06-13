package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.Serializable

@Serializable
data class Results(
    val records: List<Record> = emptyList(),
    val counters: List<Counter> = emptyList(),
)
