package com.hajira.app.domain.repository

import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import kotlinx.coroutines.flow.Flow

interface OfficeRepository {
    val office: Flow<Coordinates?>
    suspend fun save(coordinates: Coordinates)
}

interface AttendanceRepository {
    val lastCheckInMillis: Flow<Long?>
    suspend fun save(epochMillis: Long)
    suspend fun clear()
}

interface LocationRepository {
    /** Continuous high-accuracy updates. May throw [SecurityException] without permission. */
    fun updates(): Flow<LocationFix>

    /** One fresh reading, or null if the device could not produce one. */
    suspend fun currentFix(): LocationFix?
}
