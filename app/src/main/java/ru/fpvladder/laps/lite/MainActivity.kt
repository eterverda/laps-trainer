package ru.fpvladder.laps.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.fpvladder.laps.lite.ui.components.ActionButtonsRow
import ru.fpvladder.laps.lite.ui.components.ChannelWizard
import ru.fpvladder.laps.lite.ui.components.NameEditorDialog
import ru.fpvladder.laps.lite.ui.components.PilotSection
import ru.fpvladder.laps.lite.ui.screens.MainContent
import ru.fpvladder.laps.lite.ui.screens.RaceContent
import ru.fpvladder.laps.lite.ui.theme.LapsLiteTheme
import ru.fpvladder.laps.lite.viewmodel.AppScreen
import ru.fpvladder.laps.lite.viewmodel.KeyboardViewModel
import ru.fpvladder.laps.lite.viewmodel.PilotViewModel

class MainActivity : ComponentActivity() {

    private val keyboardViewModel: KeyboardViewModel by viewModels()
    private val pilotViewModel: PilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LapsLiteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppRoot(
                        keyboardViewModel = keyboardViewModel,
                        pilotViewModel = pilotViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    keyboardViewModel: KeyboardViewModel,
    pilotViewModel: PilotViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by pilotViewModel.currentScreen.collectAsState()
    val pilot by pilotViewModel.pilot.collectAsState()

    var showChannelWizard by remember { mutableStateOf(false) }
    var showNameEditor by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PilotSection(
                    pilot = pilot,
                    editable = currentScreen == AppScreen.Main,
                    onChannelClick = { showChannelWizard = true },
                    onNameClick = { showNameEditor = true }
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        when (currentScreen) {
                            AppScreen.Main -> MainContent(
                                modifier = Modifier.fillMaxSize()
                            )

                            AppScreen.Race -> RaceContent(
                                onNavigateToMain = { pilotViewModel.navigateTo(AppScreen.Main) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Text(
                    text = "Подключите USB-клавиатуру",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute"
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        ActionButtonsRow()
                    }
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                }
            }
        }
    }

    if (showChannelWizard) {
        ChannelWizard(
            currentLetter = pilot.channelLetter,
            currentNumber = pilot.channelNumber,
            currentColor = pilot.channelColor,
            onConfirm = { letter, number, color ->
                pilotViewModel.updateChannel(letter, number, color)
                showChannelWizard = false
            },
            onDismiss = { showChannelWizard = false }
        )
    }

    if (showNameEditor) {
        NameEditorDialog(
            currentName = pilot.name,
            onConfirm = { name ->
                pilotViewModel.updateName(name)
                showNameEditor = false
            },
            onDismiss = { showNameEditor = false }
        )
    }
}
