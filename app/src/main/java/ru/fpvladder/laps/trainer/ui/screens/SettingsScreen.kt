package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    channelGrid: ChannelGrid,
    colorCount: ColorCount,
    onChannelGridChange: (ChannelGrid) -> Unit,
    onColorCountChange: (ColorCount) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showGridDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

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

            Text(
                text = "Каналы",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Сетка") },
                supportingContent = { Text(channelGrid.displayName) },
                modifier = Modifier.clickable { showGridDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            ListItem(
                headlineContent = { Text("Цвета") },
                supportingContent = { Text(colorCount.displayName) },
                modifier = Modifier.clickable { showColorDialog = true }
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
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                )
                items.forEach { item ->
                    ListItem(
                        headlineContent = { Text(itemText(item)) },
                        leadingContent = {
                            RadioButton(
                                selected = item == selected,
                                onClick = { onSelect(item) }
                            )
                        },
                        modifier = Modifier.clickable { onSelect(item) }
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 24.dp)
                        .align(androidx.compose.ui.Alignment.End)
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}
