package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.BestLap
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.ui.components.BulletText
import ru.fpvladder.laps.trainer.ui.components.ScreenTitle

@Composable
fun StatsContent(
    description: AnnotatedString,
    onEditRulesClick: () -> Unit,
    stats: Stats = Stats.Individual(),
    timerPrecision: TimerPrecision = TimerPrecision.MILLISECONDS,
    isTeam: Boolean = false,
    pilot1Name: String = "",
    pilot2Name: String = "",
    pilotOrderSwapped: Boolean = false,
    onSwapPilots: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val p1Raw = pilot1Name.takeIf { it.isNotBlank() } ?: "Первый пилот"
    val p2Raw = pilot2Name.takeIf { it.isNotBlank() } ?: "Второй пилот"
    val p1 = if (pilotOrderSwapped) p2Raw else p1Raw
    val p2 = if (pilotOrderSwapped) p1Raw else p2Raw

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    ScreenTitle("Правила")
                    val paragraphs = remember(description) {
                        buildList {
                            val text = description.text
                            var start = 0
                            while (true) {
                                val idx = text.indexOf('\n', start)
                                if (idx < 0) {
                                    add(description.subSequence(start, text.length))
                                    break
                                }
                                add(description.subSequence(start, idx))
                                start = idx + 1
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        paragraphs.forEachIndexed { index, paragraph ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            Text(
                                text = paragraph,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onEditRulesClick)
                            .padding(8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать правила",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (isTeam) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onSwapPilots)
                                .padding(8.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Поменять местами",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            val flightCount = if (stats is Stats.Individual) {
                stats.counters.filterIsInstance<Counter.Builtin>()
                    .find { it.kind == Counter.Builtin.Kind.FLIGHT }?.count ?: 0
            } else 0
            val hasResults = flightCount > 0

            if (isTeam) {
                if (hasResults) {
                    PilotResultsHeader(p1)
                    ResultsInset()
                    SummaryText()
                    Spacer(modifier = Modifier.height(32.dp))
                    PilotResultsHeader(p2)
                    ResultsInset()
                    SummaryText()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                if (hasResults) {
                    ScreenTitle("Результаты")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (stats is Stats.Individual) {
                        if (stats.bestLaps.isNotEmpty()) {
                            BestLapsInset(stats.bestLaps, timerPrecision)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        CountersSummary(stats.counters)
                    } else {
                        ResultsInset()
                        SummaryText()
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PilotResultsHeader(name: String) {
    ScreenTitle(name)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SummaryText() {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "• 45 кругов\n• 1 фальстарт\n• 2 поломаных пропеллера",
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BestLapsInset(bestLaps: List<BestLap>, timerPrecision: TimerPrecision) {
    InsetCard(modifier = Modifier.fillMaxWidth()) {
        if (bestLaps.isEmpty()) {
            Text(
                text = "—",
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val visible = bestLaps
                .sortedBy { it.kind.ordinal }
                .distinctBy { it.count }
            val maxLabelLen = visible.maxOfOrNull { "${it.count}/".length } ?: 0
            val maxTimeLen = visible.maxOfOrNull {
                formatTimeDynamic(it.timeMs, timerPrecision).length
            } ?: 0
            Column(horizontalAlignment = Alignment.End) {
                visible.forEach { bestLap ->
                    val label = "${bestLap.count}/"
                    val timeStr = formatTimeDynamic(bestLap.timeMs, timerPrecision)
                    Text(
                        text = "${label.padStart(maxLabelLen)} ${timeStr.padStart(maxTimeLen)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CountersSummary(counters: List<Counter>) {
    val context = LocalContext.current
    val visible = counters.filter { it.count > 0 }
    if (visible.isEmpty()) {
        BulletText(text = "0 вылетов")
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        visible.forEach { counter ->
            val text = when (counter) {
                is Counter.Builtin -> when (counter.kind) {
                    Counter.Builtin.Kind.FLIGHT ->
                        context.resources.getQuantityString(R.plurals.flights, counter.count, counter.count)
                    Counter.Builtin.Kind.LAP ->
                        context.resources.getQuantityString(R.plurals.laps, counter.count, counter.count)
                }
                is Counter.Custom ->
                    context.getString(R.string.counter_custom, counter.count, counter.text)
            }
            BulletText(text = text)
        }
    }
}

@Composable
private fun ResultsInset() {
    val borderColor = MaterialTheme.colorScheme.outline
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val cornerRadius = 8.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    ),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
            .padding(12.dp)
    ) {
        Text(
            text = "1/   17.813 ✨",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "3/ 1:12.101 ✨",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "8/ 3:45.190 ✨",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InsetCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val cornerRadius = 8.dp.toPx()
                drawRoundRect(
                    color = outlineColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    ),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
            .padding(12.dp)
    ) {
        content()
    }
}
