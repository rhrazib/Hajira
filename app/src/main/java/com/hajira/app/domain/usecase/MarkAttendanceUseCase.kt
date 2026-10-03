package com.hajira.app.domain.usecase

import com.hajira.app.domain.model.MarkAttendanceResult
import com.hajira.app.domain.model.Proximity
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.policy.ProximityEvaluator
import com.hajira.app.domain.repository.AttendanceRepository
import com.hajira.app.domain.repository.LocationRepository
import com.hajira.app.domain.repository.OfficeRepository
import com.hajira.app.domain.util.isToday
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalTime
import kotlin.coroutines.cancellation.CancellationException

/**
 * Never trusts the enabled state of the button: it re-checks the time window, today's record,
 * and takes a brand-new GPS fix before writing anything.
 */
class MarkAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val officeRepository: OfficeRepository,
    private val locationRepository: LocationRepository,
    private val evaluator: ProximityEvaluator,
    private val policy: AttendancePolicy,
    private val clock: Clock
) {
    suspend operator fun invoke(): MarkAttendanceResult {
        if (!policy.isWindowOpen(LocalTime.now(clock))) return MarkAttendanceResult.WindowClosed

        val last = attendanceRepository.lastCheckInMillis.first()
        if (last != null && isToday(last, clock)) return MarkAttendanceResult.AlreadyMarked

        val office = officeRepository.office.first() ?: return MarkAttendanceResult.OfficeNotSet

        val fix = when (val outcome = locationRepository.fetchFix()) {
            is FixOutcome.Got -> outcome.fix
            FixOutcome.None -> return MarkAttendanceResult.NoFix
            FixOutcome.NoPermission -> return MarkAttendanceResult.PermissionMissing
            FixOutcome.Error -> return MarkAttendanceResult.Failed
        }

        return when (val proximity = evaluator.evaluate(office, fix)) {
            Proximity.MockBlocked -> MarkAttendanceResult.MockBlocked
            is Proximity.Measured -> when {
                !proximity.accurateEnough -> MarkAttendanceResult.WeakSignal(proximity.accuracyMeters)
                !proximity.withinRadius -> MarkAttendanceResult.OutOfRange(proximity.distanceMeters)
                else -> save()
            }
            Proximity.OfficeNotSet -> MarkAttendanceResult.OfficeNotSet
            Proximity.Searching -> MarkAttendanceResult.NoFix
        }
    }

    private suspend fun save(): MarkAttendanceResult = try {
        val now = clock.millis()
        attendanceRepository.save(now)
        MarkAttendanceResult.CheckedIn(now)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        MarkAttendanceResult.Failed
    }
}
