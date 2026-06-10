package ru.fpvladder.laps.trainer.model

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import ru.fpvladder.laps.trainer.R
import java.util.EnumSet

fun Training.description(context: Context): AnnotatedString {
    return when (this) {
        is Training.Team -> AnnotatedString(formatTeamPart(context, this))
        is Training.Individual -> formatIndividualAnnotated(context, this)
    }
}

private fun formatIndividualAnnotated(context: Context, training: Training.Individual): AnnotatedString {
    val first = formatIndividualFirst(context, training)
    val second = formatCalculations(training.rules.enabledBestLapKinds)
    return buildAnnotatedString {
        append(first)
        if (second.isNotBlank()) {
            append("\n")
            append(second)
        }
    }
}

private fun formatIndividualFirst(context: Context, training: Training.Individual): String {
    val rules = training.rules
    val hasTime = rules.timeLimitSeconds != Int.MAX_VALUE
    val hasLaps = rules.maxLaps != Int.MAX_VALUE

    return when {
        hasTime && hasLaps -> {
            val mins = formatMinutesString(context, rules.timeLimitSeconds / 60.0)
            val lapsUpTo = context.resources.getQuantityString(R.plurals.laps_up_to, rules.maxLaps, rules.maxLaps)
            "Летаем $mins и $lapsUpTo"
        }
        hasTime -> {
            val timePart = formatTimePart(context, rules.timeLimitSeconds)
            context.getString(R.string.flight_time_only, timePart)
        }
        hasLaps -> {
            val lapsStr = context.resources.getQuantityString(R.plurals.laps, rules.maxLaps, rules.maxLaps)
            "Летаем $lapsStr"
        }
        else -> context.getString(R.string.flight_unlimited)
    }
}

private fun formatCalculations(kinds: Set<BestLap.Kind>): String {
    val hasBest1 = BestLap.Kind.BEST_1 in kinds
    val hasBest2 = BestLap.Kind.BEST_2 in kinds
    val hasBest3 = BestLap.Kind.BEST_3 in kinds
    val hasMost = BestLap.Kind.MOST in kinds

    val parts = mutableListOf<String>()
    if (hasBest1) parts.add("лучший круг")
    if (hasBest2) parts.add("2 лучших круга подряд")
    if (hasBest3) parts.add("3 лучших круга подряд")

    val bestText = when {
        parts.isEmpty() -> ""
        parts.size == 1 -> "Считаем ${parts[0]}"
        hasBest2 && hasBest3 && !hasBest1 -> "Считаем 2 и 3 лучших круга подряд"
        parts.size == 2 -> "Считаем ${parts[0]} и ${parts[1]}"
        else -> "Считаем ${parts[0]}, 2 и 3 лучших круга подряд"
    }

    return when {
        bestText.isEmpty() && hasMost -> "Считаем максимум кругов за вылет"
        bestText.isNotEmpty() && hasMost -> "$bestText, а также максимум кругов за вылет"
        else -> bestText
    }
}

private fun formatBase(context: Context, rules: Rules): String {
    val timePart = if (rules.timeLimitSeconds != Int.MAX_VALUE) {
        formatTimePart(context, rules.timeLimitSeconds)
    } else null

    val lapsPart = if (rules.maxLaps != Int.MAX_VALUE) {
        context.resources.getQuantityString(R.plurals.laps, rules.maxLaps, rules.maxLaps)
    } else null

    return when {
        timePart != null && lapsPart != null ->
            context.getString(R.string.flight_time_and_laps, timePart, lapsPart)
        timePart != null ->
            context.getString(R.string.flight_time_only, timePart)
        lapsPart != null ->
            context.getString(R.string.flight_laps_only, lapsPart)
        else ->
            context.getString(R.string.flight_unlimited)
    }
}

private fun formatTimePart(context: Context, seconds: Int): String {
    val minutesStr = formatMinutesString(context, seconds / 60.0)
    return context.getString(R.string.flight_time_prefix, minutesStr)
}

private fun formatHalfMinutes(context: Context, seconds: Int): String {
    return formatMinutesString(context, seconds / 120.0)
}

fun formatMinutesString(context: Context, minutes: Double): String {
    val minsStr = if (minutes == minutes.toInt().toDouble()) {
        "${minutes.toInt()}"
    } else {
        String.format("%.1f", minutes).replace('.', ',')
    }
    return "$minsStr ${pluralMinutes(context, minutes)}"
}

/**
 * Функция-прослойка для склонения "минут".
 * Исключение только для русского языка: 1.5 считается как 2 (few).
 * Во всех остальных случаях используется getQuantityString.
 */
fun pluralMinutes(context: Context, minutes: Double): String {
    val quantity = when {
        minutes == 1.5 && context.resources.configuration.locales.get(0).language == "ru" -> 2
        else -> minutes.toInt()
    }
    return context.resources.getQuantityString(R.plurals.minutes, quantity)
}

private fun formatTeamPart(context: Context, training: Training.Team): String {
    val rules = training.rules
    val hasTime = rules.timeLimitSeconds != Int.MAX_VALUE
    val hasLaps = rules.maxLaps != Int.MAX_VALUE
    val halfTimeMins = if (hasTime) formatHalfMinutes(context, rules.timeLimitSeconds) else null
    val halfLaps = if (hasLaps) rules.maxLaps / 2 else null
    val rawP1 = training.pilot.name1.takeIf { it.isNotBlank() } ?: "Первый пилот"
    val rawP2 = training.pilot.name2.takeIf { it.isNotBlank() } ?: "Второй пилот"
    val p1 = if (rules.pilotOrderSwapped) rawP2 else rawP1
    val p2 = if (rules.pilotOrderSwapped) rawP1 else rawP2

    return when {
        rules.swapMode == SwapMode.TIME && halfTimeMins != null -> {
            val text = context.getString(R.string.team_swap_time, p1, halfTimeMins, p2)
            if (hasLaps) {
                val lapsStr = context.resources.getQuantityString(R.plurals.laps, rules.maxLaps, rules.maxLaps)
                context.getString(R.string.team_swap_time_with_laps, p1, halfTimeMins, p2, lapsStr)
            } else text
        }
        rules.swapMode == SwapMode.LAPS && halfLaps != null -> {
            val halfLapsStr = context.resources.getQuantityString(R.plurals.laps, halfLaps, halfLaps)
            val text = context.getString(R.string.team_swap_laps, p1, halfLapsStr, p2, halfLapsStr)
            if (hasTime) {
                val maxMins = formatMinutesString(context, rules.timeLimitSeconds / 60.0)
                context.getString(R.string.team_swap_laps_with_time, p1, halfLapsStr, p2, halfLapsStr, maxMins)
            } else text
        }
        else -> formatBase(context, rules)
    }
}
