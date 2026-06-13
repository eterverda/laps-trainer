package ru.fpvladder.laps.trainer.model

data class Results(
    val records: List<Record> = emptyList(),
    val counters: List<Counter> = emptyList(),
)
