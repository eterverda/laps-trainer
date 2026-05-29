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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val CHANNEL_GRID_KEY = stringPreferencesKey("channel_grid")
        private val COLOR_COUNT_KEY = stringPreferencesKey("color_count")
        private val IS_MUTED_KEY = booleanPreferencesKey("is_muted")
        private val USB_KEYBOARD_KEY = booleanPreferencesKey("usb_keyboard_enabled")
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
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
}
