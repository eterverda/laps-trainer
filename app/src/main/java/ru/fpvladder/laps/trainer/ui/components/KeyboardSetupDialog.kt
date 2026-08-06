package ru.fpvladder.laps.trainer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.SubcomposeLayout
import kotlinx.coroutines.flow.SharedFlow
import ru.fpvladder.laps.trainer.ui.theme.LocalExtendedColors
import ru.fpvladder.laps.trainer.usb.UsbHidCategory
import ru.fpvladder.laps.trainer.usb.UsbHidAction
import ru.fpvladder.laps.trainer.usb.UsbHidBinding
import ru.fpvladder.laps.trainer.usb.UsbHidEvent
import ru.fpvladder.laps.trainer.usb.UsbHidConfig

private data class PressedKey(
    val keyCode: Int,
    val modifiers: Int,
)

private sealed class Page {
    data object Main : Page()
    data class ActionPage(val action: UsbHidAction) : Page()
    data object Delete : Page()
}

private fun Page.toInt(): Int = when (this) {
    Page.Main -> -1
    Page.Delete -> -2
    is Page.ActionPage -> action.ordinal
}

@Composable
fun KeyboardSetupDialog(
    config: UsbHidConfig,
    productCategory: UsbHidCategory = config.info.productCategory,
    keyEvents: SharedFlow<UsbHidEvent>,
    showDeletePage: Boolean = false,
    allowEmptySave: Boolean = false,
    onSave: (UsbHidConfig) -> Unit,
    onDelete: () -> Unit = {},
    onCancel: () -> Unit,
) {
    var currentPage by remember { mutableStateOf<Page>(if (showDeletePage) Page.Delete else Page.Main) }
    var bindings by remember {
        mutableStateOf(
            config.bindings.associate { it.action to PressedKey(it.keyCode, it.modifiers) }
        )
    }
    var pressedKeys by remember { mutableStateOf(emptySet<PressedKey>()) }
    var lastPressedKey by remember { mutableStateOf<PressedKey?>(null) }

    LaunchedEffect(Unit) {
        pressedKeys = emptySet()
        lastPressedKey = null
        keyEvents.collect { event ->
            val key = PressedKey(event.keyCode, event.modifiers)
            when (event.state) {
                UsbHidEvent.STATE_DOWN -> {
                    pressedKeys = pressedKeys + key
                    lastPressedKey = key
                }
                UsbHidEvent.STATE_UP -> {
                    pressedKeys = pressedKeys - key
                    if (lastPressedKey == key) {
                        lastPressedKey = null
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(
                    text = "Клавиатура",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = config.info.displayProductName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = config.info.displayIdentity,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                SectionDivider()

                val currentState = currentPage.toInt()
                val states = remember { listOf(-2, -1) + UsbHidAction.entries.indices.toList() }
                FixedSizeAnimatedContent(
                    currentState = currentState,
                    states = states,
                    modifier = Modifier.fillMaxWidth(),
                ) { state, fillMaxHeight ->
                    when (state) {
                        -2 -> DeletePage(
                            fillMaxHeight = fillMaxHeight,
                            productCategory = productCategory,
                            onDelete = onDelete,
                            onCancel = onCancel,
                        )
                        -1 -> MainPage(
                            bindings = bindings,
                            pressedKeys = pressedKeys,
                            productCategory = productCategory,
                            allowEmptySave = allowEmptySave,
                            onActionClick = { action ->
                                currentPage = Page.ActionPage(action)
                            },
                            onSave = {
                                onSave(
                                    config.copy(
                                        bindings = bindings.map { (action, key) ->
                                            UsbHidBinding(action, key.keyCode, key.modifiers)
                                        }.toSet()
                                    )
                                )
                            },
                            onCancel = onCancel,
                        )
                        else -> {
                            val action = UsbHidAction.entries[state]
                            DetailPage(
                                action = action,
                                bindings = bindings,
                                lastPressedKey = lastPressedKey,
                                pressedKeys = pressedKeys,
                                productCategory = productCategory,
                                fillMaxHeight = fillMaxHeight,
                                onClearKey = {},
                                onApply = { key ->
                                    bindings = bindings.toMutableMap().apply {
                                        if (key == null) {
                                            remove(action)
                                        } else {
                                            entries.removeIf { it.value == key }
                                            put(action, key)
                                        }
                                    }
                                    currentPage = Page.Main
                                },
                                onCancel = { currentPage = Page.Main },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainPage(
    bindings: Map<UsbHidAction, PressedKey>,
    pressedKeys: Set<PressedKey>,
    productCategory: UsbHidCategory,
    allowEmptySave: Boolean,
    onActionClick: (UsbHidAction) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UsbHidAction.entries.forEach { action ->
            val key = bindings[action]
            MainActionRow(
                action = action,
                key = key,
                isPressed = key in pressedKeys,
                productCategory = productCategory,
                onClick = { onActionClick(action) },
            )
        }

        ActionButtons(
            dismissText = "Отмена",
            confirmText = "Сохранить",
            onDismiss = onCancel,
            onConfirm = onSave,
            confirmEnabled = allowEmptySave || bindings.isNotEmpty(),
        )
    }
}

@Composable
private fun DetailPage(
    action: UsbHidAction,
    bindings: Map<UsbHidAction, PressedKey>,
    lastPressedKey: PressedKey?,
    pressedKeys: Set<PressedKey>,
    productCategory: UsbHidCategory,
    fillMaxHeight: Boolean,
    onClearKey: () -> Unit,
    onApply: (PressedKey?) -> Unit,
    onCancel: () -> Unit,
) {
    val currentBinding = bindings[action]
    var candidateKey by remember(action) { mutableStateOf(currentBinding) }
    var pressedKeysAtOpen by remember(action) { mutableStateOf(pressedKeys) }
    val displayKey = lastPressedKey ?: candidateKey
    val isAnyPressed = lastPressedKey != null

    LaunchedEffect(pressedKeys, lastPressedKey) {
        pressedKeysAtOpen = pressedKeysAtOpen.intersect(pressedKeys)
        val newKeys = pressedKeys - pressedKeysAtOpen
        if (lastPressedKey != null && lastPressedKey in newKeys) {
            candidateKey = lastPressedKey
        }
    }

    Column(
        modifier = if (fillMaxHeight) {
            Modifier.fillMaxSize()
        } else {
            Modifier.wrapContentHeight()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(0.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                UsbHidActionIcon(
                    action = action,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = action.label(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(48.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                if (isAnyPressed) LocalExtendedColors.current.holdButtonFill else MaterialTheme.colorScheme.surfaceContainerLowest,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 12.dp, horizontal = 24.dp),
                    ) {
                        Text(
                            text = displayKey?.format(productCategory) ?: if (productCategory == UsbHidCategory.JOYSTICK) "  --  " else "   ---   ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 24.sp,
                            color = if (isAnyPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (displayKey != null) {
                        IconButton(onClick = {
                            candidateKey = null
                            onClearKey()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (productCategory) {
                    UsbHidCategory.KEYBOARD -> "Нажмите кнопку на клавиатуре"
                    UsbHidCategory.JOYSTICK -> "Нажмите кнопку на джойстике"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ActionButtons(
            dismissText = "Отмена",
            confirmText = "Применить",
            onDismiss = onCancel,
            onConfirm = { onApply(candidateKey) },
        )
    }
}

@Composable
private fun DeletePage(
    fillMaxHeight: Boolean,
    productCategory: UsbHidCategory,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = if (fillMaxHeight) {
            Modifier.fillMaxSize()
        } else {
            Modifier.wrapContentHeight()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(0.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.LinkOff,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (productCategory) {
                    UsbHidCategory.KEYBOARD -> "USB-клавиатура отключена"
                    UsbHidCategory.JOYSTICK -> "USB-джойстик отключен"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ActionButtons(
            dismissText = "Отмена",
            confirmText = "Удалить",
            onDismiss = onCancel,
            onConfirm = onDelete,
            confirmColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        )
    }
}

@Composable
private fun MainActionRow(
    action: UsbHidAction,
    key: PressedKey?,
    isPressed: Boolean,
    productCategory: UsbHidCategory,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UsbHidActionIcon(
                action = action,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = action.label(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = key?.format(productCategory) ?: if (productCategory == UsbHidCategory.JOYSTICK) "  --  " else "   ---   ",
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = if (isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(
                    if (isPressed) LocalExtendedColors.current.holdButtonFill else MaterialTheme.colorScheme.surfaceContainerLowest,
                    RoundedCornerShape(6.dp)
                )
                .padding(vertical = 6.dp, horizontal = 12.dp)
        )
    }
}

@Composable
private fun ActionButtons(
    dismissText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmColors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss) {
            Text(dismissText)
        }
        Surface(
            shape = ButtonDefaults.shape,
            color = if (confirmEnabled) confirmColors.containerColor else confirmColors.disabledContainerColor,
            contentColor = if (confirmEnabled) confirmColors.contentColor else confirmColors.disabledContentColor,
            modifier = Modifier.clickable(
                enabled = confirmEnabled,
                onClick = onConfirm,
            )
        ) {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(confirmText)
            }
        }
    }
}

@Composable
private fun FixedSizeAnimatedContent(
    currentState: Int,
    states: List<Int>,
    modifier: Modifier = Modifier,
    content: @Composable (Int, Boolean) -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val placeablesByState = states.associateWith { state ->
            subcompose(state) { content(state, false) }.map { it.measure(constraints) }
        }
        val maxWidth =
            placeablesByState.values.flatten().maxOfOrNull { it.width } ?: constraints.minWidth
        val maxHeight =
            placeablesByState.values.flatten().maxOfOrNull { it.height } ?: constraints.minHeight

        layout(maxWidth, maxHeight) {
            subcompose(AnimatedContentSlot) {
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                ) { state ->
                    content(state, true)
                }
            }.forEach {
                it.measure(Constraints(maxWidth = maxWidth, maxHeight = maxHeight)).place(0, 0)
            }
        }
    }
}

private const val AnimatedContentSlot = "animated_content"

private fun PressedKey.format(productCategory: UsbHidCategory): String = when (productCategory) {
    UsbHidCategory.KEYBOARD -> "0x%02X 0x%02X".format(modifiers and 0xFF, keyCode and 0xFF)
    UsbHidCategory.JOYSTICK -> when (modifiers and 0xFF) {
        0x01 -> "max %02d".format(keyCode and 0xFF)
        0x02 -> "min %02d".format(keyCode and 0xFF)
        0x03 -> "mid %02d".format(keyCode and 0xFF)
        else -> "btn %02d".format(keyCode and 0xFF)
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

private fun UsbHidAction.label(): String = when (this) {
    UsbHidAction.START -> "СТАРТ"
    UsbHidAction.LAP -> "КРУГ"
    UsbHidAction.ERROR -> "ОШИБКА"
    UsbHidAction.FIX -> "ИСПРАВИЛ"
    UsbHidAction.PITSTOP -> "ПИТСТОП"
    UsbHidAction.VEHICLE_LOST -> "ПОТЕРЯ"
    UsbHidAction.UNDO -> "ЗАБОЙ"
}
