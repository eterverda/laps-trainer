package ru.fpvladder.laps.trainer.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val CHANNEL_GRID_KEY = stringPreferencesKey("channel_grid")
        private val COLOR_COUNT_KEY = stringPreferencesKey("color_count")
        private val IS_MUTED_KEY = booleanPreferencesKey("is_muted")
        private val USB_KEYBOARD_KEY = booleanPreferencesKey("usb_keyboard_enabled")
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
        private val DARK_THEME_VARIANT_KEY = stringPreferencesKey("dark_theme_variant")
        private val TIMER_PRECISION_KEY = stringPreferencesKey("timer_precision")
        private val START_SIGNAL_KEY = stringPreferencesKey("start_signal")
        private val USE_LAP_BUTTON_KEY = booleanPreferencesKey("use_lap_button")
        private val USE_ERROR_FIX_BUTTONS_KEY = booleanPreferencesKey("use_error_fix_buttons")
        private val USE_PITSTOP_BUTTON_KEY = booleanPreferencesKey("use_pitstop_button")
        private val USE_VEHICLE_LOST_BUTTON_KEY = booleanPreferencesKey("use_vehicle_lost_button")
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

    val appTheme: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[APP_THEME_KEY]) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            AppThemeMode.DARK.name -> AppThemeMode.DARK
            AppThemeMode.SYSTEM.name -> AppThemeMode.SYSTEM
            "LATTE" -> AppThemeMode.LIGHT
            "FRAPPE", "MACCHIATO", "MOCHA" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    val darkThemeVariant: Flow<DarkThemeVariant> = context.dataStore.data.map { prefs ->
        prefs[DARK_THEME_VARIANT_KEY]?.let {
            runCatching { DarkThemeVariant.valueOf(it) }.getOrNull()
        } ?: DarkThemeVariant.CATPUCCIN_MOCHA
    }

    suspend fun setAppTheme(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[APP_THEME_KEY] = mode.name
        }
    }

    suspend fun setDarkThemeVariant(variant: DarkThemeVariant) {
        context.dataStore.edit { prefs ->
            prefs[DARK_THEME_VARIANT_KEY] = variant.name
        }
    }

    suspend fun migrateThemeSettings() {
        context.dataStore.edit { prefs ->
            when (val raw = prefs[APP_THEME_KEY]) {
                AppThemeMode.LIGHT.name,
                AppThemeMode.DARK.name,
                AppThemeMode.SYSTEM.name -> { /* already new format */ }
                "LATTE" -> {
                    prefs[APP_THEME_KEY] = AppThemeMode.LIGHT.name
                }
                "FRAPPE" -> {
                    prefs[APP_THEME_KEY] = AppThemeMode.DARK.name
                    prefs[DARK_THEME_VARIANT_KEY] = DarkThemeVariant.CATPUCCIN_FRAPPE.name
                }
                "MACCHIATO" -> {
                    prefs[APP_THEME_KEY] = AppThemeMode.DARK.name
                    prefs[DARK_THEME_VARIANT_KEY] = DarkThemeVariant.CATPUCCIN_MACCHIATO.name
                }
                "MOCHA" -> {
                    prefs[APP_THEME_KEY] = AppThemeMode.DARK.name
                    prefs[DARK_THEME_VARIANT_KEY] = DarkThemeVariant.CATPUCCIN_MOCHA.name
                }
                else -> {
                    prefs[APP_THEME_KEY] = AppThemeMode.SYSTEM.name
                    if (prefs[DARK_THEME_VARIANT_KEY] == null) {
                        prefs[DARK_THEME_VARIANT_KEY] = DarkThemeVariant.CATPUCCIN_MOCHA.name
                    }
                }
            }
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

    val useLapButton: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_LAP_BUTTON_KEY] ?: true
    }

    suspend fun setUseLapButton(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_LAP_BUTTON_KEY] = enabled
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

    val usePitstopButton: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_PITSTOP_BUTTON_KEY] ?: false
    }

    suspend fun setUsePitstopButton(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_PITSTOP_BUTTON_KEY] = enabled
        }
    }

    val useVehicleLostButton: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_VEHICLE_LOST_BUTTON_KEY] ?: false
    }

    suspend fun setUseVehicleLostButton(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_VEHICLE_LOST_BUTTON_KEY] = enabled
        }
    }
}
