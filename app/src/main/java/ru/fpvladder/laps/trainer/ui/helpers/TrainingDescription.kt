package ru.fpvladder.laps.trainer.ui.helpers

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Results
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.Training

fun Training.description(context: Context): AnnotatedString {
    return when (this) {
        is Training.Team -> AnnotatedString(formatTeamPart(context, this))
        is Training.Individual -> formatIndividualAnnotated(context, this)
    }
}

private fun formatIndividualAnnotated(context: Context, training: Training.Individual): AnnotatedString {
    val first = formatIndividualFirst(context, training)
    return AnnotatedString(first)
}

private fun formatIndividualFirst(context: Context, training: Training.Individual): String {
    val rules = training.rules
    val hasTime = rules.timeLimitSeconds != Int.MAX_VALUE
    val hasLaps = rules.lapsLimit != Int.MAX_VALUE

    return when {
        hasTime && hasLaps -> {
            val mins = formatMinutesString(context, rules.timeLimitSeconds / 60.0)
            val lapsUpTo = context.resources.getQuantityString(R.plurals.laps_up_to, rules.lapsLimit, rules.lapsLimit)
            "Летаем $mins и $lapsUpTo"
        }
        hasTime -> {
            val timePart = formatTimePart(context, rules.timeLimitSeconds)
            context.getString(R.string.flight_time_only, timePart)
        }
        hasLaps -> {
            val lapsStr = context.resources.getQuantityString(R.plurals.laps, rules.lapsLimit, rules.lapsLimit)
            "Летаем $lapsStr"
        }
        else -> context.getString(R.string.flight_unlimited)
    }
}

internal fun formatCalculations(kinds: Set<Record.Kind>): String {
    val hasBest1 = Record.Kind.BEST_1 in kinds
    val hasBest2 = Record.Kind.BEST_2 in kinds
    val hasBest3 = Record.Kind.BEST_3 in kinds
    val hasMost = Record.Kind.MOST in kinds

    if (!hasBest1 && !hasBest2 && !hasBest3 && !hasMost) return ""

    val parts = mutableListOf<String>()
    if (hasBest1) parts.add("лучший круг")
    if (hasBest2 && hasBest3) {
        parts.add("2 и 3 круга подряд")
    } else {
        if (hasBest2) parts.add("2 круга подряд")
        if (hasBest3) parts.add("3 круга подряд")
    }
    if (hasMost) parts.add("максимум кругов")

    return when {
        parts.size == 1 -> "Считаем ${parts[0]}"
        else -> "Считаем ${parts.dropLast(1).joinToString(", ")} и ${parts.last()}"
    }
}

private fun formatBase(context: Context, rules: Rules): String {
    val timePart = if (rules.timeLimitSeconds != Int.MAX_VALUE) {
        formatTimePart(context, rules.timeLimitSeconds)
    } else null

    val lapsPart = if (rules.lapsLimit != Int.MAX_VALUE) {
        context.resources.getQuantityString(R.plurals.laps, rules.lapsLimit, rules.lapsLimit)
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
    val hasLaps = rules.lapsLimit != Int.MAX_VALUE
    val halfTimeMins = if (hasTime) formatHalfMinutes(context, rules.timeLimitSeconds) else null
    val halfLaps = if (hasLaps) rules.lapsLimit / 2 else null
    val headPilot = training.pilot.displayNameHead(rules.swapMode, context)
    val tailPilot = training.pilot.displayNameTail(rules.swapMode, context)

    val base = when {
        rules.changeMode == Rules.Team.ChangeMode.TIME && halfTimeMins != null -> {
            val text = context.getString(R.string.team_change_time, headPilot, halfTimeMins, tailPilot)
            if (hasLaps) {
                val lapsStr = context.resources.getQuantityString(R.plurals.laps, rules.lapsLimit, rules.lapsLimit)
                context.getString(R.string.team_change_time_with_laps, headPilot, halfTimeMins, tailPilot, lapsStr)
            } else text
        }
        rules.changeMode == Rules.Team.ChangeMode.LAPS && halfLaps != null -> {
            val halfLapsStr = context.resources.getQuantityString(R.plurals.laps, halfLaps, halfLaps)
            val text = context.getString(R.string.team_change_laps, headPilot, halfLapsStr, tailPilot, halfLapsStr)
            if (hasTime) {
                val maxMins = formatMinutesString(context, rules.timeLimitSeconds / 60.0)
                context.getString(R.string.team_change_laps_with_time, headPilot, halfLapsStr, tailPilot, halfLapsStr, maxMins)
            } else text
        }
        else -> formatBase(context, rules)
    }
    return base
}
