package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.model.Rules
import ru.fpvladder.laps.trainer.model.SwapMode
import ru.fpvladder.laps.trainer.model.IndividualRulePresets
import ru.fpvladder.laps.trainer.model.TeamRulePresets

@Composable
fun RulesEditorDialog(
    currentRules: Rules,
    onConfirm: (Rules) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            RulesEditorContent(
                currentRules = currentRules,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun RulesEditorContent(
    currentRules: Rules,
    onConfirm: (Rules) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTime by rememberSaveable { mutableStateOf(currentRules.timeLimitSeconds) }
    var selectedLaps by rememberSaveable { mutableStateOf(currentRules.maxLaps) }
    var holeshot by rememberSaveable { mutableStateOf(currentRules.holeshotEnabled) }
    var swapMode by remember {
        mutableStateOf((currentRules as? Rules.Team)?.swapMode ?: SwapMode.TIME)
    }
    var optionsExpanded by rememberSaveable { mutableStateOf(false) }

    val isTeam = currentRules is Rules.Team
    val timeOptions = if (isTeam) TeamRulePresets.timeOptions else IndividualRulePresets.timeOptions
    val lapOptions = if (isTeam) TeamRulePresets.lapOptions else IndividualRulePresets.lapOptions

    val bothUnlimited = selectedTime == Int.MAX_VALUE && selectedLaps == Int.MAX_VALUE
    val timeOnly = selectedTime != Int.MAX_VALUE && selectedLaps == Int.MAX_VALUE
    val lapsOnly = selectedLaps != Int.MAX_VALUE && selectedTime == Int.MAX_VALUE
    val bothSet = selectedTime != Int.MAX_VALUE && selectedLaps != Int.MAX_VALUE

    var prevTime by remember { mutableStateOf(selectedTime) }
    var prevLaps by remember { mutableStateOf(selectedLaps) }

    if (isTeam) {
        LaunchedEffect(selectedTime, selectedLaps) {
            if (selectedTime == Int.MAX_VALUE && selectedLaps == Int.MAX_VALUE) {
                when {
                    prevTime != Int.MAX_VALUE && prevLaps == Int.MAX_VALUE -> {
                        selectedLaps = 50
                    }
                    prevLaps != Int.MAX_VALUE && prevTime == Int.MAX_VALUE -> {
                        selectedTime = 1200
                    }
                }
            }
            if (timeOnly) swapMode = SwapMode.TIME
            if (lapsOnly) swapMode = SwapMode.LAPS
            prevTime = selectedTime
            prevLaps = selectedLaps
        }
    }

    Column(
        modifier = modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionTitle("Время")
        OptionGrid(
            options = timeOptions,
            selected = selectedTime,
            onSelect = { selectedTime = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("Круги")
        OptionGrid(
            options = lapOptions,
            selected = selectedLaps,
            onSelect = { selectedLaps = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { optionsExpanded = !optionsExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Опции",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (optionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (optionsExpanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = optionsExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.offset(x = (-8).dp)) {
                        Checkbox(
                            checked = holeshot,
                            onCheckedChange = { holeshot = it }
                        )
                    }
                    Text(
                        text = "Holeshot",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (holeshot) {
                        "Время первого круга считаем от первых ворот"
                    } else {
                        "Время первого круга считаем от стартового сигнала"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 44.dp)
                )

                if (isTeam) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val swapAlpha = if (bothSet) 1f else 0.38f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.offset(x = (-8).dp)) {
                            Checkbox(
                                checked = swapMode == SwapMode.TIME,
                                onCheckedChange = { swapMode = if (it) SwapMode.TIME else SwapMode.LAPS },
                                enabled = bothSet
                            )
                        }
                        Text(
                            text = "Смена по времени",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.alpha(swapAlpha)
                        )
                    }
                    val timeVal = selectedTime
                    val lapsVal = selectedLaps
                    val swapHint = when {
                        swapMode == SwapMode.TIME && timeVal != Int.MAX_VALUE -> {
                            val half = timeVal / 2.0 / 60
                            val minsStr = if (half == half.toInt().toDouble()) "${half.toInt()}" else String.format("%.1f", half).replace('.', ',')
                            "Первый пилот летит $minsStr минут, затем летит второй пилот"
                        }
                        swapMode == SwapMode.LAPS && lapsVal != Int.MAX_VALUE -> {
                            val laps = lapsVal / 2
                            "Первый пилот летит $laps кругов, затем второй пилот летит $laps кругов"
                        }
                        else -> ""
                    }
                    if (swapHint.isNotBlank()) {
                        Text(
                            text = swapHint,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 44.dp).alpha(swapAlpha)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
            Button(
                onClick = {
                    val newRules = when (currentRules) {
                        is Rules.Individual -> Rules.Individual(
                            maxLaps = selectedLaps,
                            timeLimitSeconds = selectedTime,
                            holeshotEnabled = holeshot
                        )
                        is Rules.Team -> Rules.Team(
                            maxLaps = selectedLaps,
                            timeLimitSeconds = selectedTime,
                            holeshotEnabled = holeshot,
                            swapMode = swapMode
                        )
                    }
                    onConfirm(newRules)
                },
                enabled = !(bothUnlimited && isTeam)
            ) {
                Text("OK")
            }
        }
    }
}

@Composable
private fun OptionGrid(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { (value, label) ->
                    val fontSize = when {
                        label == "∞" -> 28.sp
                        label.length > 4 -> 14.sp
                        else -> 16.sp
                    }
                    SelectableOption(
                        text = label,
                        selected = value == selected,
                        onClick = { onSelect(value) },
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
