package com.hajira.app.domain.usecase

import com.hajira.app.domain.model.AttendanceStatus
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.model.Proximity
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.policy.ProximityEvaluator
import com.hajira.app.domain.repository.AttendanceRepository
import com.hajira.app.domain.repository.LocationRepository
import com.hajira.app.domain.repository.OfficeRepository
import com.hajira.app.domain.util.isToday
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.Clock
import java.time.LocalTime

class ObserveOfficeUseCase(private val officeRepository: OfficeRepository) {
    operator fun invoke(): Flow<Coordinates?> = officeRepository.office
}

/**
 * Live distance to the office. With [tracking] = false (no permission / GPS off) the location
 * stream is never opened, and the result is just "office not set" or "searching".
 */
class ObserveProximityUseCase(
    private val officeRepository: OfficeRepository,
    private val locationRepository: LocationRepository,
    private val evaluator: ProximityEvaluator
) {
    operator fun invoke(tracking: Boolean): Flow<Proximity> {
        val fixes: Flow<LocationFix?> = if (tracking) {
            locationRepository.updates()
                .map<LocationFix, LocationFix?> { it }
                .onStart { emit(null) }
                .catch { emit(null) }
        } else {
            flowOf(null)
        }
        return combine(officeRepository.office, fixes) { office, fix ->
            evaluator.evaluate(office, fix)
        }
    }
}

/** Today's check-in plus whether the time window is open. Re-evaluated periodically. */
class ObserveAttendanceStatusUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val policy: AttendancePolicy,
    private val clock: Clock,
    private val refreshMillis: Long = 15_000L
) {
    operator fun invoke(): Flow<AttendanceStatus> =
        combine(attendanceRepository.lastCheckInMillis, ticker()) { last, _ ->
            AttendanceStatus(
                checkedInAtMillis = last?.takeIf { isToday(it, clock) },
                windowOpen = policy.isWindowOpen(LocalTime.now(clock))
            )
        }

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(refreshMillis)
        }
    }
}
