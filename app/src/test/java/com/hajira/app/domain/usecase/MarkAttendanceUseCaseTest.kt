package com.hajira.app.domain.usecase

import com.hajira.app.domain.fakes.FakeAttendanceRepository
import com.hajira.app.domain.fakes.FakeLocationRepository
import com.hajira.app.domain.fakes.FakeOfficeRepository
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.model.MarkAttendanceResult
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.policy.ProximityEvaluator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MarkAttendanceUseCaseTest {

    private val office = Coordinates(23.8103, 90.4125)
    private val morning = Clock.fixed(Instant.parse("2026-10-02T09:30:00Z"), ZoneOffset.UTC)

    private fun fix(deltaLat: Double, accuracy: Float = 5f) =
        LocationFix(Coordinates(office.latitude + deltaLat, office.longitude), accuracy, false)

    private class Env(
        val attendance: FakeAttendanceRepository,
        val location: FakeLocationRepository,
        val useCase: MarkAttendanceUseCase
    )

    private fun env(
        fix: LocationFix? = null,
        officeSet: Boolean = true,
        lastCheckIn: Long? = null,
        policy: AttendancePolicy = AttendancePolicy(),
        clock: Clock = morning,
        error: Exception? = null
    ): Env {
        val attendance = FakeAttendanceRepository(lastCheckIn)
        val location = FakeLocationRepository(fix = fix, error = error)
        val offices = FakeOfficeRepository(if (officeSet) office else null)
        return Env(
            attendance,
            location,
            MarkAttendanceUseCase(attendance, offices, location, ProximityEvaluator(policy), policy, clock)
        )
    }

    @Test
    fun insideRadius_checksIn_andPersists() = runTest {
        val e = env(fix = fix(0.0003))
        val result = e.useCase()
        assertTrue(result is MarkAttendanceResult.CheckedIn)
        assertEquals(morning.millis(), e.attendance.state.value)
    }

    @Test
    fun outsideRadius_isRejected_andNothingIsSaved() = runTest {
        val e = env(fix = fix(0.0010))
        val result = e.useCase()
        assertTrue(result is MarkAttendanceResult.OutOfRange)
        assertNull(e.attendance.state.value)
    }

    @Test
    fun weakAccuracy_isRejected() = runTest {
        val e = env(fix = fix(0.0001, accuracy = 90f))
        assertTrue(e.useCase() is MarkAttendanceResult.WeakSignal)
    }

    @Test
    fun alreadyCheckedInToday_isRejected() = runTest {
        val e = env(fix = fix(0.0), lastCheckIn = morning.millis() - 60_000)
        assertEquals(MarkAttendanceResult.AlreadyMarked, e.useCase())
    }

    @Test
    fun checkInFromYesterday_doesNotBlockToday() = runTest {
        val yesterday = morning.millis() - 24 * 60 * 60 * 1000L
        val e = env(fix = fix(0.0), lastCheckIn = yesterday)
        assertTrue(e.useCase() is MarkAttendanceResult.CheckedIn)
        assertNotNull(e.attendance.state.value)
    }

    @Test
    fun noOffice_isRejected() = runTest {
        val e = env(fix = fix(0.0), officeSet = false)
        assertEquals(MarkAttendanceResult.OfficeNotSet, e.useCase())
    }

    @Test
    fun noFix_isReported() = runTest {
        assertEquals(MarkAttendanceResult.NoFix, env(fix = null).useCase())
    }

    @Test
    fun missingPermission_isReported() = runTest {
        val e = env(error = SecurityException("no permission"))
        assertEquals(MarkAttendanceResult.PermissionMissing, e.useCase())
    }

    @Test
    fun unexpectedError_isReported() = runTest {
        val e = env(error = IllegalStateException("boom"))
        assertEquals(MarkAttendanceResult.Failed, e.useCase())
    }

    @Test
    fun outsideTimeWindow_isRejected_whenEnforced() = runTest {
        val early = Clock.fixed(Instant.parse("2026-10-02T08:00:00Z"), ZoneOffset.UTC)
        val e = env(fix = fix(0.0), policy = AttendancePolicy(enforceTimeWindow = true), clock = early)
        assertEquals(MarkAttendanceResult.WindowClosed, e.useCase())
    }
}
