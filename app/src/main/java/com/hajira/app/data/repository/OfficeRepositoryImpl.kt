package com.hajira.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import com.hajira.app.data.local.safeData
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class OfficeRepositoryImpl(private val dataStore: DataStore<Preferences>) : OfficeRepository {

    override val office: Flow<Coordinates?> = dataStore.safeData
        .map { prefs ->
            val lat = prefs[OFFICE_LAT]
            val lng = prefs[OFFICE_LNG]
            if (lat != null && lng != null) Coordinates(lat, lng) else null
        }
        .distinctUntilChanged()

    override suspend fun save(coordinates: Coordinates) {
        dataStore.edit {
            it[OFFICE_LAT] = coordinates.latitude
            it[OFFICE_LNG] = coordinates.longitude
        }
    }

    private companion object {
        val OFFICE_LAT = doublePreferencesKey("office_lat")
        val OFFICE_LNG = doublePreferencesKey("office_lng")
    }
}
