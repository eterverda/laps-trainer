package ru.fpvladder.laps.trainer.ui.helpers

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules

fun Pilot.Team.displayName1(context: Context): String {
    return when {
        name1.isNotBlank() -> name1
        else -> context.getString(R.string.team_pilot_1)
    }
}

fun Pilot.Team.displayName2(context: Context): String {
    return when {
        name2.isNotBlank() -> name2
        else -> context.getString(R.string.team_pilot_2)
    }
}

fun Pilot.Team.displayNameHead(swapMode: Rules.Team.SwapMode, context: Context): String {
    return when (swapMode) {
        Rules.Team.SwapMode.SWAPPED -> displayName2(context)
        Rules.Team.SwapMode.STRAIGHT -> displayName1(context)
    }
}

fun Pilot.Team.displayNameTail(swapMode: Rules.Team.SwapMode, context: Context): String {
    return when (swapMode) {
        Rules.Team.SwapMode.SWAPPED -> displayName1(context)
        Rules.Team.SwapMode.STRAIGHT -> displayName2(context)
    }
}

fun Pilot.Individual.label(@Suppress("unused") context: Context): AnnotatedString {
    return when {
        name.isNotBlank() -> AnnotatedString(name)
        else -> buildAnnotatedString { appendLapsTrainer() }
    }
}

fun Pilot.Team.label1(@Suppress("unused") context: Context): AnnotatedString {
    return when {
        name1.isNotBlank() -> AnnotatedString(name1)
        else -> buildAnnotatedString { appendLapsTrainer() }
    }
}

fun Pilot.Team.label2(context: Context): AnnotatedString {
    return when {
        name2.isNotBlank() -> AnnotatedString(name2)
        else -> buildAnnotatedString { append(context.getString(R.string.team_competition)) }
    }
}

private fun AnnotatedString.Builder.appendLapsTrainer() {
    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
        append("Laps")
    }
    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
        append(".Trainer")
    }
}
