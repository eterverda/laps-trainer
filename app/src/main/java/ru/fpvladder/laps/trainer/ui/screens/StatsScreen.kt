package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.trainer.R
import ru.fpvladder.laps.trainer.model.Pilot
import ru.fpvladder.laps.trainer.model.Counter
import ru.fpvladder.laps.trainer.model.Record
import ru.fpvladder.laps.trainer.model.Stats
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.ui.helpers.displayName1
import ru.fpvladder.laps.trainer.ui.helpers.displayName2
import ru.fpvladder.laps.trainer.ui.helpers.timeMs
import ru.fpvladder.laps.trainer.ui.components.BulletText
import ru.fpvladder.laps.trainer.ui.components.MeasuredHorizontalPager
import ru.fpvladder.laps.trainer.ui.components.ScreenTitle

@Composable
fun StatsScreen(
    description: AnnotatedString,
    onEditRulesClick: () -> Unit,
    stats: Stats = Stats.Individual(),
    showRecordKinds: Set<Record.Kind> = emptySet(),
    timerPrecision: TimerPrecision = TimerPrecision.MILLISECONDS,
    pilot: Pilot,
    onRotatePilots: () -> Unit = {},
    isDefault: Boolean = true,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var boxHeight by remember { mutableIntStateOf(0) }
    var contentHeight by remember { mutableIntStateOf(0) }
    var buttonsHeight by remember { mutableIntStateOf(0) }
    val gapPx = with(density) { 16.dp.roundToPx() }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { boxHeight = it.height }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.onSizeChanged { contentHeight = it.height }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(start = 24.dp, end = 12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                            ) {
                                ScreenTitle("Правила")

                                Text(
                                    text = description,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            Box(modifier = Modifier.fillMaxHeight()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-6).dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onEditRulesClick)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Редактировать правила",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(24.dp)
                                    )
                                }
                                if (pilot is Pilot.Team) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .offset(y = 6.dp)
                                            .clip(CircleShape)
                                            .clickable(onClick = onRotatePilots)
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Поменять местами",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(if (pilot is Pilot.Team) 16.dp else 40.dp))

                        val flightCount = stats.results.counters
                            .find { it.kind == Counter.Builtin.Kind.FLIGHT }?.count ?: 0
                        val hasResults = flightCount > 0

                        if (hasResults) {
                            if (pilot is Pilot.Team && stats is Stats.Team) {
                                TeamStatsContent(
                                    stats = stats,
                                    timerPrecision = timerPrecision,
                                    showRecordKinds = showRecordKinds,
                                    pilot1Name = pilot.displayName1(context),
                                    pilot2Name = pilot.displayName2(context)
                                )
                            } else if (stats is Stats.Individual) {
                                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                                    ScreenTitle("Результаты")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val visibleRecords = stats.results.records.filter { it.kind in showRecordKinds }
                                    if (visibleRecords.isNotEmpty()) {
                                        RecordsInset(visibleRecords.distinctBy { it.count }, timerPrecision)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    CountersSummary(stats.results.counters)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                val spacerHeight = with(density) {
                    val paddingPx = 16.dp.roundToPx()
                    val remaining = boxHeight - contentHeight - buttonsHeight - gapPx - paddingPx
                    if (remaining > 0 && contentHeight > 0 && buttonsHeight > 0) remaining.toDp() else 16.dp
                }
                Spacer(modifier = Modifier.height(spacerHeight))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { buttonsHeight = it.height },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isDefault) {
                        TextButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Завершить тренировку",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TeamStatsContent(
    stats: Stats.Team,
    showRecordKinds: Set<Record.Kind>,
    timerPrecision: TimerPrecision,
    pilot1Name: String,
    pilot2Name: String
) {
    val pages: List<Pair<String, @Composable () -> Unit>> = listOf(
        "Результаты" to @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ScreenTitle("Результаты")
                Spacer(modifier = Modifier.height(8.dp))
                val commonVisibleRecords = stats.results.records.filter { it.kind in showRecordKinds }
                if (commonVisibleRecords.isNotEmpty()) {
                    RecordsInset(commonVisibleRecords.distinctBy { it.count }, timerPrecision)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CountersSummary(stats.results.counters)
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        pilot1Name to @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ScreenTitle("Результаты: $pilot1Name")
                Spacer(modifier = Modifier.height(8.dp))
                val firstRecords = stats.first.total.records.filter { it.kind in showRecordKinds }
                val firstRecordsBeingHead = stats.first.head.records.filter { it.kind in showRecordKinds }
                val firstRecordsBeingTail = stats.first.tail.records.filter { it.kind in showRecordKinds }
                val firstHasRecords = firstRecords.isNotEmpty() ||
                    firstRecordsBeingHead.isNotEmpty() ||
                    firstRecordsBeingTail.isNotEmpty()
                if (firstHasRecords) {
                    PilotRecordsInset(
                        records = firstRecords.distinctBy { it.count },
                        recordsBeingHead = firstRecordsBeingHead.distinctBy { it.count },
                        recordsBeingTail = firstRecordsBeingTail.distinctBy { it.count },
                        timerPrecision = timerPrecision
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CountersSummary(stats.first.total.counters)
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        pilot2Name to @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ScreenTitle("Результаты: $pilot2Name")
                Spacer(modifier = Modifier.height(8.dp))
                val secondRecords = stats.second.total.records.filter { it.kind in showRecordKinds }
                val secondRecordsBeingHead = stats.second.head.records.filter { it.kind in showRecordKinds }
                val secondRecordsBeingTail = stats.second.tail.records.filter { it.kind in showRecordKinds }
                val secondHasRecords = secondRecords.isNotEmpty() ||
                    secondRecordsBeingHead.isNotEmpty() ||
                    secondRecordsBeingTail.isNotEmpty()
                if (secondHasRecords) {
                    PilotRecordsInset(
                        records = secondRecords.distinctBy { it.count },
                        recordsBeingHead = secondRecordsBeingHead.distinctBy { it.count },
                        recordsBeingTail = secondRecordsBeingTail.distinctBy { it.count },
                        timerPrecision = timerPrecision
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CountersSummary(stats.second.total.counters)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    PageIndicator(
        pageCount = pages.size,
        currentPage = pagerState.currentPage,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    MeasuredHorizontalPager(
        state = pagerState,
        pageCount = pages.size,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        pages[page].second()
    }
}

@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val color = if (index == currentPage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun PilotRecordsInset(
    records: List<Record>,
    recordsBeingHead: List<Record>,
    recordsBeingTail: List<Record>,
    timerPrecision: TimerPrecision
) {
    val allRecords = remember(records, recordsBeingHead, recordsBeingTail) {
        records.map { it to null } +
            recordsBeingHead.map { it to "летел первым" } +
            recordsBeingTail.map { it to "летел вторым" }
    }
    if (allRecords.isEmpty()) return

    val maxLabelLen = allRecords.maxOfOrNull { "${it.first.count}/".length } ?: 0
    val maxTimeLen = allRecords.maxOfOrNull {
        formatTimeDynamic(it.first.timeMs(timerPrecision), timerPrecision).length
    } ?: 0

    InsetCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            allRecords.forEach { (record, postfix) ->
                val label = "${record.count}/".padStart(maxLabelLen)
                val timeStr = formatTimeDynamic(record.timeMs(timerPrecision), timerPrecision).padStart(maxTimeLen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$label $timeStr",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (postfix != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($postfix)",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
fun CountersSummary(counters: List<Counter>) {
    val context = LocalContext.current
    if (counters.isEmpty()) {
        BulletText(text = "0 вылетов")
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        counters.forEach { counter ->
            val text = when (val kind = counter.kind) {
                Counter.Builtin.Kind.FLIGHT ->
                    context.resources.getQuantityString(R.plurals.flights, counter.count, counter.count)
                Counter.Builtin.Kind.LAP ->
                    context.resources.getQuantityString(R.plurals.laps, counter.count, counter.count)
                null ->
                    context.getString(R.string.counter_custom, counter.count, (counter as Counter.Custom).text)
            }
            BulletText(text = text)
        }
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

@Composable
fun RecordsInset(
    records: List<Record>,
    timerPrecision: TimerPrecision,
    postfix: String? = null
) {
    if (records.isNotEmpty()) {
        InsetCard(modifier = Modifier.fillMaxWidth()) {
            val visible = records.sortedBy { it.kind.ordinal }
            val maxLabelLen = visible.maxOfOrNull { "${it.count}/".length } ?: 0
            val maxTimeLen = visible.maxOfOrNull {
                formatTimeDynamic(it.timeMs(timerPrecision), timerPrecision).length
            } ?: 0
            Column(modifier = Modifier.fillMaxWidth()) {
                visible.forEach { record ->
                    val label = "${record.count}/"
                    val timeStr = formatTimeDynamic(record.timeMs(timerPrecision), timerPrecision)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${label.padStart(maxLabelLen)} ${timeStr.padStart(maxTimeLen)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (postfix != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "($postfix)",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
