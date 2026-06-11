package ru.fpvladder.laps.trainer.model

import java.util.UUID

sealed class Training {
    abstract val id: String
    abstract val pilot: Pilot
    abstract val rules: Rules
    abstract val stats: Stats
    abstract val flights: List<Flight>
    abstract val createdAt: Long
    abstract val isArchived: Boolean

    fun withArchived(archived: Boolean): Training = when (this) {
        is Individual -> copy(isArchived = archived)
        is Team -> copy(isArchived = archived)
    }

    class Individual(
        override val id: String = UUID.randomUUID().toString(),
        override val pilot: Pilot.Individual = Pilot.Individual(),
        override val rules: Rules.Individual = Rules.Individual(),
        override val stats: Stats = Stats.Individual(),
        override val flights: List<Flight.Individual> = emptyList(),
        override val createdAt: Long = System.currentTimeMillis(),
        override val isArchived: Boolean = false
    ) : Training() {
        fun copy(
            id: String = this.id,
            pilot: Pilot.Individual = this.pilot,
            rules: Rules.Individual = this.rules,
            stats: Stats = this.stats,
            flights: List<Flight.Individual> = this.flights,
            createdAt: Long = this.createdAt,
            isArchived: Boolean = this.isArchived
        ) = Individual(id, pilot, rules, stats, flights, createdAt, isArchived)
    }

    class Team(
        override val id: String = UUID.randomUUID().toString(),
        override val pilot: Pilot.Team = Pilot.Team(),
        override val rules: Rules.Team = Rules.Team(),
        override val stats: Stats = Stats.Team(),
        override val flights: List<Flight.Team> = emptyList(),
        override val createdAt: Long = System.currentTimeMillis(),
        override val isArchived: Boolean = false
    ) : Training() {
        fun copy(
            id: String = this.id,
            pilot: Pilot.Team = this.pilot,
            rules: Rules.Team = this.rules,
            stats: Stats = this.stats,
            flights: List<Flight.Team> = this.flights,
            createdAt: Long = this.createdAt,
            isArchived: Boolean = this.isArchived
        ) = Team(id, pilot, rules, stats, flights, createdAt, isArchived)
    }
}
