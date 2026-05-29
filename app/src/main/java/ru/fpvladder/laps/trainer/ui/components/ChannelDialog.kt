package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.model.Channel
import ru.fpvladder.laps.trainer.model.ChannelColor
import ru.fpvladder.laps.trainer.model.ChannelConfig
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount

@Composable
fun ChannelDialog(
    currentChannel: Channel,
    channelGrid: ChannelGrid,
    colorCount: ColorCount,
    showApplyToAll: Boolean = false,
    onConfirm: (Channel) -> Unit,
    onConfirmForAll: ((Channel) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ChannelEditorContent(
                currentChannel = currentChannel,
                channelGrid = channelGrid,
                colorCount = colorCount,
                showApplyToAll = showApplyToAll,
                onConfirm = onConfirm,
                onConfirmForAll = onConfirmForAll,
                onDismiss = onDismiss
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChannelEditorContent(
    currentChannel: Channel,
    channelGrid: ChannelGrid,
    colorCount: ColorCount,
    onConfirm: (Channel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    showApplyToAll: Boolean = false,
    onConfirmForAll: ((Channel) -> Unit)? = null,
    onValuesChange: ((Channel) -> Unit)? = null
) {
    var selectedLetter by rememberSaveable { mutableStateOf(currentChannel.letter) }
    var selectedNumber by rememberSaveable { mutableStateOf(currentChannel.number) }
    var selectedColor by rememberSaveable { mutableStateOf(currentChannel.color) }

    val isAnalog = channelGrid == ChannelGrid.ANALOG
    val allChannels = if (!isAnalog) {
        ChannelConfig.availableLetters(channelGrid).flatMap { letter ->
            ChannelConfig.availableNumbers(channelGrid, letter).map { number ->
                letter to number
            }
        }
    } else emptyList()

    val isValid = ChannelConfig.isValidChannel(channelGrid, selectedLetter, selectedNumber) &&
            ChannelConfig.isValidColor(colorCount, selectedColor)

    Column(
        modifier = modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isAnalog) {
            SectionTitle("Сетка")
            LetterGrid(
                selected = selectedLetter,
                onSelect = {
                    selectedLetter = it
                    onValuesChange?.invoke(Channel(selectedLetter, selectedNumber, selectedColor))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SectionTitle("Канал")
            NumberGrid(
                selected = selectedNumber,
                onSelect = {
                    selectedNumber = it
                    onValuesChange?.invoke(Channel(selectedLetter, selectedNumber, selectedColor))
                }
            )
        } else {
            SectionTitle("Канал")
            ChannelGrid(
                channels = allChannels,
                selectedLetter = selectedLetter,
                selectedNumber = selectedNumber,
                onSelect = { letter, number ->
                    selectedLetter = letter
                    selectedNumber = number
                    onValuesChange?.invoke(Channel(selectedLetter, selectedNumber, selectedColor))
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionTitle("Цвет")
        ColorGrid(
            selected = selectedColor,
            colorCount = colorCount,
            onSelect = {
                selectedColor = it
                onValuesChange?.invoke(Channel(selectedLetter, selectedNumber, selectedColor))
            }
        )

        if (showActions) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }
                Spacer(modifier = Modifier.width(8.dp))
                val buttonColors = ButtonDefaults.buttonColors()
                Surface(
                    shape = ButtonDefaults.shape,
                    color = if (isValid) buttonColors.containerColor else buttonColors.disabledContainerColor,
                    contentColor = if (isValid) buttonColors.contentColor else buttonColors.disabledContentColor,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = {
                                if (isValid) {
                                    onConfirm(Channel(selectedLetter, selectedNumber, selectedColor))
                                }
                            },
                            onLongClick = {
                                if (isValid) {
                                    onConfirmForAll?.invoke(Channel(selectedLetter, selectedNumber, selectedColor))
                                }
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OK")
                    }
                }
            }

            if (showApplyToAll) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Долгое нажатие на кнопку ОК изменит канал для всех",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun LetterGrid(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("R", "L").forEach { letter ->
                SelectableItem(
                    text = letter,
                    selected = letter == selected,
                    onClick = { onSelect(letter) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(2f))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("A", "B", "E", "F").forEach { letter ->
                SelectableItem(
                    text = letter,
                    selected = letter == selected,
                    onClick = { onSelect(letter) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NumberGrid(selected: Int, onSelect: (Int) -> Unit) {
    val numbers = ChannelColor.numbers
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        numbers.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { number ->
                    SelectableItem(
                        text = number.toString(),
                        selected = number == selected,
                        onClick = { onSelect(number) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelGrid(
    channels: List<Pair<String, Int>>,
    selectedLetter: String,
    selectedNumber: Int,
    onSelect: (String, Int) -> Unit
) {
    val grouped = channels.groupBy { if (it.first == "E") "F" else it.first }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        grouped.values.forEachIndexed { index, group ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            group.chunked(4).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { (letter, number) ->
                        SelectableItem(
                            text = "$letter$number",
                            selected = letter == selectedLetter && number == selectedNumber,
                            onClick = { onSelect(letter, number) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorGrid(
    selected: ChannelColor,
    colorCount: ColorCount,
    onSelect: (ChannelColor) -> Unit
) {
    val colors = ChannelConfig.availableColors(colorCount)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { color ->
                    ColorItem(
                        color = color,
                        selected = color == selected,
                        onClick = { onSelect(color) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
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
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorItem(
    color: ChannelColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasOutline = color.outlineColor != null
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.color,
        border = BorderStroke(
            width = if (selected) 3.dp else if (hasOutline) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else color.outlineColor ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // No text, just color
        }
    }
}
