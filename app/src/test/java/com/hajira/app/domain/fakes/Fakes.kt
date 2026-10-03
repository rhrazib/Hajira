package com.hajira.app.domain.fakes

import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.repository.AttendanceRepository
import com.hajira.app.domain.repository.LocationRepository
import com.hajira.app.domain.repository.OfficeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

class FakeOfficeRepository(initial: Coordinates? = null) : OfficeRepository {
    val state = MutableStateFlow(initial)
    override val office: Flow<Coordinates?> = state
    override suspend fun save(coordinates: Coordinates) {
        state.value = coordinates
    }
}

class FakeAttendanceRepository(initial: Long? = null) : AttendanceRepository {
    val state = MutableStateFlow(initial)
    override val lastCheckInMillis: Flow<Long?> = state
    override suspend fun save(epochMillis: Long) {
        state.value = epochMillis
    }
    override suspend fun clear() {
        state.value = null
    }
}

class FakeLocationRepository(
    var fix: LocationFix? = null,
    var error: Exception? = null,
    var stream: Flow<LocationFix> = emptyFlow()
) : LocationRepository {
    override fun updates(): Flow<LocationFix> = stream
    override suspend fun currentFix(): LocationFix? {
        error?.let { throw it }
        return fix
    }
}
