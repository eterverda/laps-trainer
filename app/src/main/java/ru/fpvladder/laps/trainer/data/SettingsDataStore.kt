package ru.fpvladder.laps.trainer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.fpvladder.laps.trainer.model.ChannelGrid
import ru.fpvladder.laps.trainer.model.ColorCount

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val CHANNEL_GRID_KEY = stringPreferencesKey("channel_grid")
        private val COLOR_COUNT_KEY = stringPreferencesKey("color_count")
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
}
