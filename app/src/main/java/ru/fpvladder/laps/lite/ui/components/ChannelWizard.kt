package ru.fpvladder.laps.lite.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.lite.model.ChannelColor

@Composable
fun ChannelWizard(
    currentLetter: String,
    currentNumber: Int,
    currentColor: ChannelColor,
    onConfirm: (String, Int, ChannelColor) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLetter by rememberSaveable { mutableStateOf(currentLetter) }
    var selectedNumber by rememberSaveable { mutableStateOf(currentNumber) }
    var selectedColor by rememberSaveable { mutableStateOf(currentColor) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                SectionTitle("Сетка")
                LetterGrid(
                    selected = selectedLetter,
                    onSelect = { selectedLetter = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SectionTitle("Канал")
                NumberGrid(
                    selected = selectedNumber,
                    onSelect = { selectedNumber = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SectionTitle("Цвет")
                ColorGrid(
                    selected = selectedColor,
                    onSelect = { selectedColor = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(selectedLetter, selectedNumber, selectedColor)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
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
private fun ColorGrid(selected: ChannelColor, onSelect: (ChannelColor) -> Unit) {
    val colors = ChannelColor.entries.take(4)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        colors.forEach { color ->
            ColorItem(
                color = color,
                selected = color == selected,
                onClick = { onSelect(color) },
                modifier = Modifier.weight(1f)
            )
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
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
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
