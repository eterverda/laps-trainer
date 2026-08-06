package ru.fpvladder.laps.trainer.ui.components

import ru.fpvladder.laps.trainer.ui.screens.formatTimeDynamic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Lap
import ru.fpvladder.laps.trainer.viewmodel.LapMark
import ru.fpvladder.laps.trainer.model.NO_PILOT_CHANGE
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.ui.helpers.displayName1
import ru.fpvladder.laps.trainer.ui.helpers.displayName2
import ru.fpvladder.laps.trainer.ui.helpers.displayNameHead
import ru.fpvladder.laps.trainer.ui.helpers.displayNameTail
import ru.fpvladder.laps.trainer.ui.helpers.label
import ru.fpvladder.laps.trainer.ui.helpers.runningLabel

@Composable
internal fun LapList(
    laps: List<Lap>,
    currentLap: Lap?,
    currentLapTime: Long,
    timerPrecision: TimerPrecision,
    isPostFlight: Boolean,
    lapMarks: Map<Int, List<LapMark>> = emptyMap(),
    pilot: Pilot? = null,
    pilotChangeIndex: Int = NO_PILOT_CHANGE,
    swapMode: Rules.Team.SwapMode = Rules.Team.SwapMode.STRAIGHT,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    @Composable
    fun LapRow(
        label: String,
        time: String,
        isFailed: Boolean,
        isPitstop: Boolean,
        marks: List<LapMark>,
        maxLabelLen: Int,
        maxTimeLen: Int
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${label.padStart(maxLabelLen)} ${time.padStart(maxTimeLen)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isFailed) TextDecoration.LineThrough else null
            )
            if (marks.isNotEmpty()) {
                marks.forEach { mark ->
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(
                            when (mark) {
                                LapMark.ERROR -> R.drawable.ic_cross
                                LapMark.FIX -> R.drawable.ic_square
                                LapMark.PITSTOP -> R.drawable.ic_pitstop
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                if (isPitstop) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_pitstop),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (isFailed) {
                    Spacer(Modifier.width(if (isPitstop) 4.dp else 8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_cross),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }

    val currentVisible = currentLap.takeIf { !isPostFlight }

    val maxLabelLen = remember(laps, currentVisible) {
        val candidates = laps.map { it.label } +
                listOfNotNull(currentVisible?.runningLabel)
        candidates.maxOfOrNull { it.length } ?: 0
    }

    val allTimes = remember(laps, currentVisible, currentLapTime, timerPrecision) {
        laps.map { formatTimeDynamic(it.durationMs, timerPrecision) } +
                listOfNotNull(currentVisible?.let { formatTimeDynamic(currentLapTime, timerPrecision) })
    }
    val maxTimeLen = allTimes.maxOfOrNull { it.length } ?: 0

    Column(
        modifier = modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End
    ) {
        if (pilot is Pilot.Team) {
            val startingPilot = pilot.displayNameHead(swapMode, context)
            if (startingPilot.isNotBlank()) {
                Text(
                    text = "Стартует $startingPilot",
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        laps.forEachIndexed { index, lap ->
            LapRow(
                label = lap.label,
                time = formatTimeDynamic(lap.durationMs, timerPrecision),
                isFailed = !lap.success,
                isPitstop = lap.pitstop,
                marks = lapMarks[index].orEmpty(),
                maxLabelLen = maxLabelLen,
                maxTimeLen = maxTimeLen
            )
            if (pilot is Pilot.Team && pilotChangeIndex >= 0 && index == pilotChangeIndex) {
                val nextPilot = pilot.displayNameTail(swapMode, context)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Смена пилота. Стартует $nextPilot",
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        currentVisible?.let { lap ->
            LapRow(
                label = lap.runningLabel,
                time = formatTimeDynamic(currentLapTime, timerPrecision),
                isFailed = false,
                isPitstop = false,
                marks = lapMarks[laps.size].orEmpty(),
                maxLabelLen = maxLabelLen,
                maxTimeLen = maxTimeLen
            )
        }
        if (pilot is Pilot.Team && pilotChangeIndex >= 0 && pilotChangeIndex == laps.size) {
            val nextPilot = pilot.displayNameTail(swapMode, context)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Смена пилота. Стартует $nextPilot",
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
