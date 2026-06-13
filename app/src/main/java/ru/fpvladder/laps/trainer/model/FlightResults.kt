package ru.fpvladder.laps.trainer.model

fun computeIndividualFlight(
    laps: List<Lap>,
    stopReason: StopReason
): Flight.Individual {
    return Flight.Individual(
        laps = laps,
        stopReason = stopReason,
        result = Results(
            records = computeFlightRecords(laps),
            counters = computeFlightCounters(laps)
        )
    )
}

private fun computeFlightRecords(
    laps: List<Lap>
): List<Record> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    val completedLaps = laps.filter { it.status != Lap.Status.HS }

    if (validLaps.isEmpty() && completedLaps.isEmpty()) {
        return emptyList()
    }

    val records = mutableListOf<Record>()

    if (validLaps.isNotEmpty()) {
        val best1 = validLaps.minByOrNull { it.timeMs }!!
        records.add(
            Record(
                count = 1,
                kind = Record.Kind.BEST_1,
                intervals = listOf(best1.interval)
            )
        )
    }

    if (validLaps.size >= 2) {
        val size = minOf(2, validLaps.size)
        val windowLaps = minWindowLaps(validLaps, size)
        records.add(
            Record(
                count = size,
                kind = Record.Kind.BEST_2,
                intervals = windowLaps.map { it.interval }
            )
        )
    }

    if (validLaps.size >= 3) {
        val size = minOf(3, validLaps.size)
        val windowLaps = minWindowLaps(validLaps, size)
        records.add(
            Record(
                count = size,
                kind = Record.Kind.BEST_3,
                intervals = windowLaps.map { it.interval }
            )
        )
    }

    if (completedLaps.isNotEmpty()) {
        records.add(
            Record(
                count = completedLaps.size,
                kind = Record.Kind.MOST,
                intervals = completedLaps.map { it.interval }
            )
        )
    }

    return records.toList()
}

private fun computeFlightCounters(laps: List<Lap>): List<Counter> {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    return listOf(
        Counter.Builtin(
            count = validLaps.size,
            kind = Counter.Builtin.Kind.LAP
        )
    )
}

fun computeTeamFlight(
    laps: List<Lap>,
    stopReason: StopReason,
    pilotSwapIndex: Int?
): Flight.Team {
    val validLaps = laps.filter { it.status == Lap.Status.SUCCESS }
    val completedLaps = laps.filter { it.status != Lap.Status.HS }

    val headPilotRaw = if (pilotSwapIndex != null) laps.take(pilotSwapIndex + 1) else laps
    val tailPilotRaw = if (pilotSwapIndex != null) laps.drop(pilotSwapIndex + 1) else emptyList()

    val headPilotSuccessLaps = headPilotRaw.filter { it.status == Lap.Status.SUCCESS }
    val tailPilotSuccessLaps = tailPilotRaw.filter { it.status == Lap.Status.SUCCESS }

    val headPilotCompletedLaps = headPilotRaw.filter { it.status != Lap.Status.HS }
    val tailPilotCompletedLaps = tailPilotRaw.filter { it.status != Lap.Status.HS }

    val commonRecords = mutableListOf<Record>()
    val headRecords = mutableListOf<Record>()
    val tailRecords = mutableListOf<Record>()

    if (completedLaps.isNotEmpty()) {
        commonRecords.add(
            Record(
                count = validLaps.size,
                kind = Record.Kind.MOST,
                intervals = completedLaps.map { it.interval }
            )
        )
    }
    if (headPilotCompletedLaps.isNotEmpty()) {
        headRecords.add(
            Record(
                count = headPilotSuccessLaps.size,
                kind = Record.Kind.MOST,
                intervals = headPilotCompletedLaps.map { it.interval }
            )
        )
    }
    if (tailPilotCompletedLaps.isNotEmpty()) {
        tailRecords.add(
            Record(
                count = tailPilotSuccessLaps.size,
                kind = Record.Kind.MOST,
                intervals = tailPilotCompletedLaps.map { it.interval }
            )
        )
    }
    if (headPilotSuccessLaps.isNotEmpty()) {
        headPilotSuccessLaps.minByOrNull { it.timeMs }?.let {
            headRecords.add(
                Record(
                    count = 1,
                    kind = Record.Kind.BEST_1,
                    intervals = listOf(it.interval)
                )
            )
        }
    }
    if (tailPilotSuccessLaps.isNotEmpty()) {
        tailPilotSuccessLaps.minByOrNull { it.timeMs }?.let {
            tailRecords.add(
                Record(
                    count = 1,
                    kind = Record.Kind.BEST_1,
                    intervals = listOf(it.interval)
                )
            )
        }
    }

    return Flight.Team(
        laps = laps,
        stopReason = stopReason,
        pilotSwapIndex = pilotSwapIndex,
        common = Results(
            records = commonRecords,
            counters = listOf(
                Counter.Builtin(
                    count = validLaps.size,
                    kind = Counter.Builtin.Kind.LAP
                )
            )
        ),
        head = Results(
            records = headRecords,
            counters = listOf(
                Counter.Builtin(
                    count = headPilotSuccessLaps.size,
                    kind = Counter.Builtin.Kind.LAP
                )
            )
        ),
        tail = Results(
            records = tailRecords,
            counters = if (tailPilotSuccessLaps.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    Counter.Builtin(
                        count = tailPilotSuccessLaps.size,
                        kind = Counter.Builtin.Kind.LAP
                    )
                )
            }
        )
    )
}

private fun minWindowLaps(
    laps: List<Lap>,
    size: Int
): List<Lap> {
    if (size >= laps.size) {
        return laps
    }
    var minSum = Long.MAX_VALUE
    var minIndex = 0
    for (i in 0..laps.size - size) {
        var sum = 0L
        for (j in 0 until size) {
            sum += laps[i + j].timeMs
        }
        if (sum < minSum) {
            minSum = sum
            minIndex = i
        }
    }
    return laps.subList(minIndex, minIndex + size)
}
