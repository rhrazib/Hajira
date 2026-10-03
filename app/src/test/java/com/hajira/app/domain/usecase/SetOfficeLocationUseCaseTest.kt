package com.hajira.app.domain.usecase

import com.hajira.app.domain.fakes.FakeLocationRepository
import com.hajira.app.domain.fakes.FakeOfficeRepository
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.model.SetOfficeResult
import com.hajira.app.domain.policy.AttendancePolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetOfficeLocationUseCaseTest {

    private val here = Coordinates(23.8103, 90.4125)
    private val offices = FakeOfficeRepository()

    private suspend fun attempt(
        fix: LocationFix?,
        error: Exception? = null,
        policy: AttendancePolicy = AttendancePolicy()
    ): SetOfficeResult =
        SetOfficeLocationUseCase(FakeLocationRepository(fix, error), offices, policy)()

    @Test
    fun goodFix_isSaved() = runTest {
        assertEquals(SetOfficeResult.Saved(here), attempt(LocationFix(here, 8f, false)))
        assertEquals(here, offices.state.value)
    }

    @Test
    fun weakFix_isRejected_andNotSaved() = runTest {
        assertTrue(attempt(LocationFix(here, 120f, false)) is SetOfficeResult.WeakSignal)
        assertNull(offices.state.value)
    }

    @Test
    fun noFix_isReported() = runTest {
        assertEquals(SetOfficeResult.NoFix, attempt(null))
    }

    @Test
    fun missingPermission_isReported() = runTest {
        assertEquals(SetOfficeResult.PermissionMissing, attempt(null, SecurityException("denied")))
    }

    @Test
    fun mockFix_isRejected_whenBlocked() = runTest {
        val result = attempt(LocationFix(here, 5f, true), policy = AttendancePolicy(blockMockLocation = true))
        assertEquals(SetOfficeResult.MockBlocked, result)
    }
}
