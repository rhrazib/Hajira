package com.hajira.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

internal val Context.hajiraDataStore: DataStore<Preferences> by preferencesDataStore(name = "hajira_prefs")

/** A corrupted or unreadable file should look like "nothing saved", not crash the app. */
internal val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }
