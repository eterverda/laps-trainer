package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.model.AppTheme
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.TimerPrecision
import ru.fpvladder.laps.trainer.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    channelGrid: ChannelGrid,
    colorCount: ColorCount,
    isMuted: Boolean,
    isUsbKeyboardEnabled: Boolean,
    useErrorFixButtons: Boolean,
    appTheme: AppTheme,
    timerPrecision: TimerPrecision,
    startSignal: StartSignal,
    onChannelGridChange: (ChannelGrid) -> Unit,
    onColorCountChange: (ColorCount) -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onUsbKeyboardChange: (Boolean) -> Unit,
    onUseErrorFixButtonsChange: (Boolean) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onTimerPrecisionChange: (TimerPrecision) -> Unit,
    onStartSignalChange: (StartSignal) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showGridDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrecisionDialog by remember { mutableStateOf(false) }
    var showSignalDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Поддержка USB-клавиатуры") },
                trailingContent = {
                    Switch(
                        checked = isUsbKeyboardEnabled,
                        onCheckedChange = onUsbKeyboardChange
                    )
                }
            )

            SectionDivider()

            ListItem(
                headlineContent = { Text("Беззвучный режим") },
                trailingContent = {
                    Switch(
                        checked = isMuted,
                        onCheckedChange = onMutedChange
                    )
                }
            )

            SectionDivider()

            ListItem(
                headlineContent = { Text("Кнопки Ошибка/Исправил") },
                trailingContent = {
                    Switch(
                        checked = useErrorFixButtons,
                        onCheckedChange = onUseErrorFixButtonsChange
                    )
                }
            )

            SectionDivider()

            ListItem(
                headlineContent = { Text("Сигнал на старт") },
                supportingContent = { Text(startSignal.displayName) },
                modifier = Modifier.clickable { showSignalDialog = true }
            )
            ListItem(
                headlineContent = { Text("Точность ручной засечки") },
                supportingContent = { Text(timerPrecision.displayName) },
                modifier = Modifier.clickable { showPrecisionDialog = true }
            )

            SectionDivider()

            ListItem(
                headlineContent = { Text("Сетка") },
                supportingContent = { Text(channelGrid.displayName) },
                modifier = Modifier.clickable { showGridDialog = true }
            )
            ListItem(
                headlineContent = { Text("Цвета") },
                supportingContent = { Text(colorCount.displayName) },
                modifier = Modifier.clickable { showColorDialog = true }
            )

            SectionDivider()

            ListItem(
                headlineContent = { Text("Тема") },
                supportingContent = { Text(appTheme.displayName) },
                modifier = Modifier.clickable { showThemeDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showGridDialog) {
        SelectionDialog(
            title = "Сетка",
            items = ChannelGrid.entries,
            selected = channelGrid,
            itemText = { it.displayName },
            onSelect = {
                onChannelGridChange(it)
                showGridDialog = false
            },
            onDismiss = { showGridDialog = false }
        )
    }

    if (showColorDialog) {
        SelectionDialog(
            title = "Цвета",
            items = ColorCount.entries,
            selected = colorCount,
            itemText = { it.displayName },
            onSelect = {
                onColorCountChange(it)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = "Тема",
            items = AppTheme.entries,
            selected = appTheme,
            itemText = { it.displayName },
            onSelect = {
                onAppThemeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showPrecisionDialog) {
        SelectionDialog(
            title = "Точность ручной засечки",
            items = TimerPrecision.entries,
            selected = timerPrecision,
            itemText = { it.displayName },
            onSelect = {
                onTimerPrecisionChange(it)
                showPrecisionDialog = false
            },
            onDismiss = { showPrecisionDialog = false }
        )
    }

    if (showSignalDialog) {
        SelectionDialog(
            title = "Сигнал на старт",
            items = StartSignal.entries,
            selected = startSignal,
            itemText = { it.displayName },
            onSelect = {
                onStartSignalChange(it)
                showSignalDialog = false
            },
            onDismiss = { showSignalDialog = false }
        )
    }
}

@Composable
private fun SectionDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    items: List<T>,
    selected: T,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                SectionTitle(
                    text = title,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp)
                )
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onSelect(item) }
                            .padding(start = 16.dp, end = 24.dp)
                    ) {
                        RadioButton(
                            selected = item == selected,
                            onClick = { onSelect(item) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = itemText(item),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}
