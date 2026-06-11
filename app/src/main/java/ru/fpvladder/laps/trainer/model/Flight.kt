package ru.fpvladder.laps.trainer.model

import java.util.UUID

sealed class Flight {
    abstract val id: String
    abstract val trainingId: String
    abstract val pilot: Pilot
    abstract val rules: Rules
    abstract val laps: List<Lap>
    abstract val stopReason: StopReason
    abstract val counters: List<Counter>
    abstract val records: List<Record>
    abstract val createdAt: Long
    abstract val completedAt: Long?

    data class Individual(
        override val id: String = UUID.randomUUID().toString(),
        override val trainingId: String,
        override val pilot: Pilot.Individual,
        override val rules: Rules.Individual,
        override val laps: List<Lap> = emptyList(),
        override val stopReason: StopReason = StopReason.MANUAL,
        override val records: List<Record> = emptyList(),
        override val counters: List<Counter> = emptyList(),
        override val createdAt: Long = System.currentTimeMillis(),
        override val completedAt: Long? = null
    ) : Flight()

    data class Team(
        override val id: String = UUID.randomUUID().toString(),
        override val trainingId: String,
        override val pilot: Pilot.Team,
        override val rules: Rules.Team,
        override val laps: List<Lap> = emptyList(),
        override val stopReason: StopReason = StopReason.MANUAL,
        val pilotSwapIndex: Int? = null,
        val common: Item = Item(),
        val head: Item = Item(),
        val tail: Item = Item(),
        override val createdAt: Long = System.currentTimeMillis(),
        override val completedAt: Long? = null
    ) : Flight() {
        override val counters: List<Counter> get() = common.counters
        override val records: List<Record> get() = common.records

        data class Item(
            val counters: List<Counter> = emptyList(),
            val records: List<Record> = emptyList(),
        )
    }
}
