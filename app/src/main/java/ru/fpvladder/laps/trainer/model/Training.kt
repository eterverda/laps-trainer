package ru.fpvladder.laps.trainer.model

import java.util.Objects
import java.util.UUID

sealed class Training(
    val id: String,
) {
    abstract val pilot: Pilot
    abstract val rules: Rules
    abstract val stats: Stats
    abstract val flights: List<Flight>

    class Individual private constructor(
        id: String,
        override val pilot: Pilot.Individual,
        override val rules: Rules.Individual,
        override val stats: Stats.Individual,
        override val flights: List<Flight.Individual>,
    ) : Training(id) {

        constructor(
            pilot: Pilot.Individual = Pilot.Individual(),
        ) : this(
            id = UUID.randomUUID().toString(),
            pilot = pilot,
            rules = Rules.Individual(),
            stats = Stats.Individual(),
            flights = emptyList(),
        )

        fun copy(
            pilot: Pilot.Individual = this.pilot,
            rules: Rules.Individual = this.rules,
            stats: Stats.Individual = this.stats,
            flights: List<Flight.Individual> = this.flights
        ) = Individual(
            id = id,
            pilot = pilot,
            rules = rules,
            stats = stats,
            flights = flights
        )
    }

    class Team private constructor(
        id: String,
        override val pilot: Pilot.Team = Pilot.Team(),
        override val rules: Rules.Team = Rules.Team(),
        override val stats: Stats.Team = Stats.Team(),
        override val flights: List<Flight.Team> = emptyList(),
    ) : Training(id = id) {

        constructor(
            pilot: Pilot.Team = Pilot.Team(),
        ) : this(
            id = UUID.randomUUID().toString(),
            pilot = pilot,
            rules = Rules.Team(),
            stats = Stats.Team(),
            flights = emptyList(),
        )

        fun copy(
            pilot: Pilot.Team = this.pilot,
            rules: Rules.Team = this.rules,
            stats: Stats.Team = this.stats,
            flights: List<Flight.Team> = this.flights
        ) = Team(
            id = id,
            pilot = pilot,
            rules = rules,
            stats = stats,
            flights = flights
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Training) return false
        return id == other.id &&
                pilot == other.pilot &&
                rules == other.rules &&
                stats == other.stats &&
                flights == other.flights
    }

    override fun hashCode(): Int = Objects.hash(id, pilot, rules, stats, flights)

    override fun toString(): String =
        "${javaClass.simpleName}(id=$id, pilot=$pilot, rules=$rules, stats=$stats, flights.size=${flights.size})"
}
