package ru.fpvladder.laps.trainer.model

const val NO_PILOT_CHANGE = -1

fun computeIndividualFlight(
    laps: List<Lap>,
    stopReason: StopReason
): Flight.Individual {
    return Flight.Individual(
        laps = laps,
        stopReason = stopReason
    )
}

fun computeTeamFlight(
    laps: List<Lap>,
    stopReason: StopReason,
    pilotChangeIndex: Int,
    swapMode: Rules.Team.SwapMode
): Flight.Team {
    val head: List<Lap>
    val tail: List<Lap>
    if (pilotChangeIndex < 0 || pilotChangeIndex >= laps.size) {
        head = laps
        tail = emptyList()
    } else {
        head = laps.take(pilotChangeIndex + 1)
        tail = laps.drop(pilotChangeIndex + 1)
    }
    return Flight.Team(
        headLaps = head,
        tailLaps = tail,
        stopReason = stopReason,
        swapMode = swapMode
    )
}

internal fun computeFlightResults(laps: List<Lap>): Results {
    return Results(
        records = computeFlightRecords(laps),
        counters = computeFlightCounters(laps)
    )
}

internal fun computeTeamCommonResults(laps: List<Lap>): Results {
    val validLaps = laps.filter { it.success && it.number > 0 }
    val completedLaps = laps.filter { it.number > 0 }

    val records = if (completedLaps.isEmpty()) {
        emptyList()
    } else {
        listOf(
            Record(
                count = validLaps.size,
                kind = Record.Kind.MOST,
                intervals = completedLaps.map { it.interval }
            )
        )
    }

    val counters = if (validLaps.isEmpty()) {
        emptyList()
    } else {
        listOf(
            Counter.Builtin(
                count = validLaps.size,
                kind = Counter.Builtin.Kind.LAP
            )
        )
    }

    return Results(records, counters)
}

private fun computeFlightRecords(
    laps: List<Lap>
): List<Record> {
    val validLaps = laps.filter { it.success && it.number > 0 }
    val completedLaps = laps.filter { it.number > 0 }

    if (validLaps.isEmpty() && completedLaps.isEmpty()) {
        return emptyList()
    }

    val records = mutableListOf<Record>()

    if (validLaps.isNotEmpty()) {
        val best1 = validLaps.minByOrNull { it.durationMs }!!
        records.add(
            Record(
                count = 1,
                kind = Record.Kind.BEST_1,
                intervals = listOf(best1.interval)
            )
        )
    }

    if (completedLaps.size >= 2) {
        val size = 2
        minWindowLaps(completedLaps, size)?.let { windowLaps ->
            records.add(
                Record(
                    count = size,
                    kind = Record.Kind.BEST_2,
                    intervals = windowLaps.map { it.interval }
                )
            )
        }
    }

    if (completedLaps.size >= 3) {
        val size = 3
        minWindowLaps(completedLaps, size)?.let { windowLaps ->
            records.add(
                Record(
                    count = size,
                    kind = Record.Kind.BEST_3,
                    intervals = windowLaps.map { it.interval }
                )
            )
        }
    }

    if (completedLaps.isNotEmpty()) {
        records.add(
            Record(
                count = validLaps.size,
                kind = Record.Kind.MOST,
                intervals = completedLaps.map { it.interval }
            )
        )
    }

    return records.toList()
}

private fun computeFlightCounters(laps: List<Lap>): List<Counter> {
    val validLaps = laps.filter { it.success && it.number > 0 }
    return listOf(
        Counter.Builtin(
            count = validLaps.size,
            kind = Counter.Builtin.Kind.LAP
        )
    )
}

private fun minWindowLaps(
    laps: List<Lap>,
    size: Int
): List<Lap>? {
    return laps.windowed(size)
        .filter { window -> window.all { it.success } }
        .minByOrNull { window -> window.sumOf { it.durationMs } }
}
