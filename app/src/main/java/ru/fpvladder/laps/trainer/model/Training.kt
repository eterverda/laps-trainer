package ru.fpvladder.laps.trainer.model

import java.util.UUID

sealed class Training {
    abstract val id: String
    abstract val rules: Rules
    abstract val stats: Stats
    abstract val createdAt: Long
    abstract val isArchived: Boolean

    val description: String
        get() {
            val timePart = if (rules.timeLimitSeconds != Int.MAX_VALUE) {
                formatTimePart(rules.timeLimitSeconds)
            } else null
            val lapsPart = if (rules.maxLaps != Int.MAX_VALUE) {
                "${rules.maxLaps} круг${ending(rules.maxLaps)}"
            } else null
            val base = when {
                timePart != null && lapsPart != null -> "$timePart, $lapsPart"
                timePart != null -> timePart
                lapsPart != null -> lapsPart
                else -> "Летаем без ограничений"
            }
            val teamPart = if (this is Training.Team) {
                val hasTime = rules.timeLimitSeconds != Int.MAX_VALUE
                val hasLaps = rules.maxLaps != Int.MAX_VALUE
                val halfTimeMins = if (hasTime) formatHalfMinutes(rules.timeLimitSeconds) else null
                val halfLaps = if (hasLaps) rules.maxLaps / 2 else null
                val rawP1 = pilot.name1.takeIf { it.isNotBlank() } ?: "Первый пилот"
                val rawP2 = pilot.name2.takeIf { it.isNotBlank() } ?: "Второй пилот"
                val p1 = if (rules.pilotOrderSwapped) rawP2 else rawP1
                val p2 = if (rules.pilotOrderSwapped) rawP1 else rawP2
                when {
                    rules.swapMode == SwapMode.TIME && halfTimeMins != null -> {
                        val text = "$p1 летит $halfTimeMins, затем летит $p2"
                        if (hasLaps) "$text. Максимум ${rules.maxLaps} круг${ending(rules.maxLaps)}." else text
                    }
                    rules.swapMode == SwapMode.LAPS && halfLaps != null -> {
                        val text = "$p1 летит $halfLaps круг${ending(halfLaps)}, затем $p2 летит $halfLaps круг${ending(halfLaps)}"
                        if (hasTime) {
                            val maxMins = rules.timeLimitSeconds / 60
                            "$text. Максимум $maxMins ${minutesPlural(maxMins)}."
                        } else text
                    }
                    else -> base
                }
            } else null
            return teamPart ?: base
        }

    private fun formatMinutesString(minutes: Double): String {
        val minsStr = if (minutes == minutes.toInt().toDouble()) {
            "${minutes.toInt()}"
        } else {
            String.format("%.1f", minutes).replace('.', ',')
        }
        val plural = if (minutes == minutes.toInt().toDouble()) {
            minutesPlural(minutes.toInt())
        } else if (minutes.toInt() == 1) {
            "минуты"
        } else {
            minutesPlural(minutes.toInt())
        }
        return "$minsStr $plural"
    }

    private fun formatTimePart(seconds: Int): String {
        return "Летаем ${formatMinutesString(seconds / 60.0)}"
    }

    private fun formatHalfMinutes(seconds: Int): String {
        return formatMinutesString(seconds / 120.0)
    }

    private fun minutesPlural(count: Int): String = when {
        count % 10 == 1 && count % 100 != 11 -> "минута"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "минуты"
        else -> "минут"
    }

    private fun ending(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> ""
        n % 10 in 2..4 && n % 100 !in 12..14 -> "а"
        else -> "ов"
    }

    fun withArchived(archived: Boolean): Training = when (this) {
        is Individual -> copy(isArchived = archived)
        is Team -> copy(isArchived = archived)
    }

    class Individual(
        override val id: String = UUID.randomUUID().toString(),
        val pilot: Pilot.Individual = Pilot.Individual(),
        override val rules: Rules.Individual = Rules.Individual(),
        override val stats: Stats = Stats.Individual(),
        override val createdAt: Long = System.currentTimeMillis(),
        override val isArchived: Boolean = false
    ) : Training() {
        fun copy(
            id: String = this.id,
            pilot: Pilot.Individual = this.pilot,
            rules: Rules.Individual = this.rules,
            stats: Stats = this.stats,
            createdAt: Long = this.createdAt,
            isArchived: Boolean = this.isArchived
        ) = Individual(id, pilot, rules, stats, createdAt, isArchived)
    }

    class Team(
        override val id: String = UUID.randomUUID().toString(),
        val pilot: Pilot.Team = Pilot.Team(),
        override val rules: Rules.Team = Rules.Team(),
        override val stats: Stats = Stats.Team(),
        override val createdAt: Long = System.currentTimeMillis(),
        override val isArchived: Boolean = false
    ) : Training() {
        fun copy(
            id: String = this.id,
            pilot: Pilot.Team = this.pilot,
            rules: Rules.Team = this.rules,
            stats: Stats = this.stats,
            createdAt: Long = this.createdAt,
            isArchived: Boolean = this.isArchived
        ) = Team(id, pilot, rules, stats, createdAt, isArchived)
    }
}
