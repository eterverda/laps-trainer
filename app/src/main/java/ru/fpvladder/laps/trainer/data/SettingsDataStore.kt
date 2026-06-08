package ru.fpvladder.laps.trainer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.fpvladder.laps.trainer.model.AppTheme
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount
import ru.fpvladder.laps.trainer.model.StartSignal
import ru.fpvladder.laps.trainer.model.TimerPrecision

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val CHANNEL_GRID_KEY = stringPreferencesKey("channel_grid")
        private val COLOR_COUNT_KEY = stringPreferencesKey("color_count")
        private val IS_MUTED_KEY = booleanPreferencesKey("is_muted")
        private val USB_KEYBOARD_KEY = booleanPreferencesKey("usb_keyboard_enabled")
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
        private val TIMER_PRECISION_KEY = stringPreferencesKey("timer_precision")
        private val START_SIGNAL_KEY = stringPreferencesKey("start_signal")
        private val USE_ERROR_FIX_BUTTONS_KEY = booleanPreferencesKey("use_error_fix_buttons")
    }

    val channelGrid: Flow<ChannelGrid> = context.dataStore.data.map { prefs ->
        prefs[CHANNEL_GRID_KEY]?.let {
            runCatching { ChannelGrid.valueOf(it) }.getOrNull()
        } ?: ChannelGrid.HDZERO
    }

    val colorCount: Flow<ColorCount> = context.dataStore.data.map { prefs ->
        prefs[COLOR_COUNT_KEY]?.let {
            runCatching { ColorCount.valueOf(it) }.getOrNull()
        } ?: ColorCount.FOUR
    }

    suspend fun setChannelGrid(grid: ChannelGrid) {
        context.dataStore.edit { prefs ->
            prefs[CHANNEL_GRID_KEY] = grid.name
        }
    }

    suspend fun setColorCount(count: ColorCount) {
        context.dataStore.edit { prefs ->
            prefs[COLOR_COUNT_KEY] = count.name
        }
    }

    val isMuted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_MUTED_KEY] ?: false
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_MUTED_KEY] = muted
        }
    }

    val isUsbKeyboardEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USB_KEYBOARD_KEY] ?: true
    }

    suspend fun setUsbKeyboardEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USB_KEYBOARD_KEY] = enabled
        }
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        prefs[APP_THEME_KEY]?.let {
            runCatching { AppTheme.valueOf(it) }.getOrNull()
        } ?: AppTheme.SYSTEM
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[APP_THEME_KEY] = theme.name
        }
    }

    val timerPrecision: Flow<TimerPrecision> = context.dataStore.data.map { prefs ->
        prefs[TIMER_PRECISION_KEY]?.let {
            runCatching { TimerPrecision.valueOf(it) }.getOrNull()
        } ?: TimerPrecision.DECISECONDS
    }

    suspend fun setTimerPrecision(precision: TimerPrecision) {
        context.dataStore.edit { prefs ->
            prefs[TIMER_PRECISION_KEY] = precision.name
        }
    }

    val startSignal: Flow<StartSignal> = context.dataStore.data.map { prefs ->
        prefs[START_SIGNAL_KEY]?.let {
            runCatching { StartSignal.valueOf(it) }.getOrNull()
        } ?: StartSignal.RANDOM
    }

    suspend fun setStartSignal(signal: StartSignal) {
        context.dataStore.edit { prefs ->
            prefs[START_SIGNAL_KEY] = signal.name
        }
    }

    val useErrorFixButtons: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_ERROR_FIX_BUTTONS_KEY] ?: false
    }

    suspend fun setUseErrorFixButtons(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_ERROR_FIX_BUTTONS_KEY] = enabled
        }
    }
}
