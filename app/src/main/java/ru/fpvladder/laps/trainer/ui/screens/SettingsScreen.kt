package ru.fpvladder.laps.trainer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.fpvladder.laps.trainer.settings.AppTheme
import ru.fpvladder.laps.trainer.settings.ChannelGrid
import ru.fpvladder.laps.trainer.settings.ColorCount
import ru.fpvladder.laps.trainer.settings.StartSignal
import ru.fpvladder.laps.trainer.settings.TimerPrecision
import ru.fpvladder.laps.trainer.BuildConfig
import ru.fpvladder.laps.trainer.R
import androidx.core.net.toUri
import ru.fpvladder.laps.trainer.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    channelGrid: ChannelGrid,
    colorCount: ColorCount,
    isMuted: Boolean,
    isUsbKeyboardEnabled: Boolean,
    isUsbFeatureEnabled: Boolean = false,
    useLapButton: Boolean,
    useErrorFixButtons: Boolean,
    appTheme: AppTheme,
    timerPrecision: TimerPrecision,
    startSignal: StartSignal,
    onChannelGridChange: (ChannelGrid) -> Unit,
    onColorCountChange: (ColorCount) -> Unit,
    onMutedChange: (Boolean) -> Unit,
    onUsbKeyboardChange: (Boolean) -> Unit,
    onUseLapButtonChange: (Boolean) -> Unit,
    onUseErrorFixButtonsChange: (Boolean) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onTimerPrecisionChange: (TimerPrecision) -> Unit,
    onStartSignalChange: (StartSignal) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) "+debug" else ""

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showGridDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrecisionDialog by remember { mutableStateOf(false) }
    var showSignalDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (isUsbFeatureEnabled) {
                    ListItem(
                        headlineContent = { Text("Поддержка USB-клавиатуры") },
                        trailingContent = {
                            CompactSwitch(
                                checked = isUsbKeyboardEnabled,
                                onCheckedChange = onUsbKeyboardChange
                            )
                        }
                    )

                    SectionDivider()
                }

                ListItem(
                    headlineContent = { Text("Беззвучный режим") },
                    trailingContent = {
                        CompactSwitch(
                            checked = isMuted,
                            onCheckedChange = onMutedChange
                        )
                    }
                )

                SectionDivider()

                ListItem(
                    headlineContent = {
                        FlowRow(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Кнопка ", modifier = Modifier.align(Alignment.CenterVertically))
                            InlineButton(
                                text = "Круг",
                                iconRes = R.drawable.ic_circle
                            )
                            Text(
                                if (useLapButton) " видна" else " скрыта",
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    },
                    trailingContent = {
                        CompactSwitch(
                            checked = useLapButton,
                            onCheckedChange = onUseLapButtonChange
                        )
                    }
                )
                AnimatedVisibility(visible = !useLapButton) {
                    Text(
                        text = "Нажимайте на экран чтобы засчитать круг",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ListItem(
                    headlineContent = {
                        FlowRow(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Кнопки ", modifier = Modifier.align(Alignment.CenterVertically))
                            InlineButton(
                                text = "Ошибка",
                                iconRes = R.drawable.ic_cross
                            )
                            Text(" и ", modifier = Modifier.align(Alignment.CenterVertically))
                            InlineButton(
                                text = "Исправил",
                                iconRes = R.drawable.ic_square
                            )
                            Text(
                                if (useErrorFixButtons) " видны" else " скрыты",
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    },
                    trailingContent = {
                        CompactSwitch(
                            checked = useErrorFixButtons,
                            onCheckedChange = onUseErrorFixButtonsChange
                        )
                    }
                )
                AnimatedVisibility(visible = useErrorFixButtons) {
                    Text(
                        text = "Нажимайте Ошибка когда пилот сошел с траектории. Нажимайте Исправил, когда пилот вернулся на траекторию",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SectionDivider()

                ListItem(
                    headlineContent = { Text("Сигнал на старт") },
                    supportingContent = {
                        Text(
                            if (isMuted) {
                                "В беззвучном режиме только Ручной"
                            } else {
                                startSignal.displayName
                            }
                        )
                    },
                    modifier = Modifier.clickable(enabled = !isMuted) { showSignalDialog = true },
                    colors = ListItemDefaults.colors(
                        headlineColor = if (isMuted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        supportingColor = if (isMuted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
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

                SectionDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                append("Laps")
                            }
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(".Trainer")
                            }
                            append(" ver. $versionName")
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (BuildConfig.FLAVOR == "rustore") {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_rustore),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RuStore Edition",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (BuildConfig.FLAVOR == "vip") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFD700),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = BuildConfig.VIP_BADGE_TEXT.takeIf(String::isNotBlank)
                                        ?: "VIP Edition",
                                    color = Color.Black,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "© 2026 NOOB@WHOOP",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://pay.cloudtips.ru/p/0c51e214".toUri()
                                )
                            )
                        },
                        modifier = Modifier.padding(top = 40.dp),
                    ) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Донат автору на")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_cloudtips),
                            contentDescription = "CloudTips",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CloudTips")
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
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
            itemDescription = { it.description },
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
            itemDescription = { it.description },
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(3.dp)
                .background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun InlineButton(
    text: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(vertical = 3.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(text = text)
        }
    }
}

@Composable
private fun CompactSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    items: List<T>,
    selected: T,
    itemText: (T) -> String,
    itemDescription: ((T) -> String)? = null,
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
                            .height(if (itemDescription != null) 64.dp else 48.dp)
                            .clickable { onSelect(item) }
                            .padding(start = 16.dp, end = 24.dp)
                    ) {
                        RadioButton(
                            selected = item == selected,
                            onClick = { onSelect(item) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = itemText(item),
                                style = MaterialTheme.typography.labelLarge
                            )
                            itemDescription?.let { description ->
                                Text(
                                    text = description(item),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
