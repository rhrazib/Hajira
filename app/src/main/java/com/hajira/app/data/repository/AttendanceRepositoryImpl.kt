package com.hajira.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.hajira.app.data.local.safeData
import com.hajira.app.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AttendanceRepositoryImpl(private val dataStore: DataStore<Preferences>) : AttendanceRepository {

    override val lastCheckInMillis: Flow<Long?> = dataStore.safeData
        .map { it[LAST_CHECK_IN] }
        .distinctUntilChanged()

    override suspend fun save(epochMillis: Long) {
        dataStore.edit { it[LAST_CHECK_IN] = epochMillis }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(LAST_CHECK_IN) }
    }

    private companion object {
        val LAST_CHECK_IN = longPreferencesKey("last_check_in")
    }
}
