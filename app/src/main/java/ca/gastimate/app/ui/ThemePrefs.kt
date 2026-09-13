package ca.gastimate.app.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "gastimate_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

// Two stored preferences: app theme + favourited station ids.
object ThemePrefs {
    private val KEY = stringPreferencesKey("theme_mode")
    private val FAVORITES = stringSetPreferencesKey("favorite_stations")

    fun mode(context: Context): Flow<ThemeMode> =
        context.dataStore.data.map { prefs ->
            when (prefs[KEY]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit { it[KEY] = mode.name }
    }

    fun favorites(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { it[FAVORITES] ?: emptySet() }

    suspend fun toggleFavorite(context: Context, stationId: String) {
        context.dataStore.edit { prefs ->
            val cur = prefs[FAVORITES] ?: emptySet()
            prefs[FAVORITES] = if (stationId in cur) cur - stationId else cur + stationId
        }
    }
}
