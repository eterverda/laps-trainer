package ru.fpvladder.laps.trainer.model

import kotlinx.serialization.Serializable

data class Record(
    val count: Int,
    val kind: Kind,
    val intervals: List<TimeInterval> = emptyList()
) {
    @Serializable
    enum class Kind { BEST_1, BEST_2, BEST_3, MOST }

    fun rawTimeMs(): Long = intervals.sumOf { it.durationMs }

    val allSuccess: Boolean
        get() {
            if (intervals.size != count) return false
            if (intervals.size <= 1) return true
            for (i in 0 until intervals.size - 1) {
                if (intervals[i].endMs != intervals[i + 1].startMs) return false
            }
            return true
        }

    fun isBetterThan(other: Record): Boolean {
        return count > other.count || (count == other.count && rawTimeMs() < other.rawTimeMs())
    }
}

internal fun mergeRecordLists(
    existing: List<Record>,
    new: List<Record>
): List<Record> {
    if (new.isEmpty()) return existing
    val bestMap = existing.associateBy { it.kind }.toMutableMap()
    new.forEach { record ->
        val current = bestMap[record.kind]
        if (current == null || record.isBetterThan(current)) {
            bestMap[record.kind] = record
        }
    }
    return bestMap.values.toList().sortedBy { it.kind.ordinal }
}
