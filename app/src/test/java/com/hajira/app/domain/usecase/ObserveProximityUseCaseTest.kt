package com.hajira.app.domain.usecase

import com.hajira.app.domain.fakes.FakeLocationRepository
import com.hajira.app.domain.fakes.FakeOfficeRepository
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.model.Proximity
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.policy.ProximityEvaluator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveProximityUseCaseTest {

    private val office = Coordinates(23.8103, 90.4125)
    private val nearby = LocationFix(Coordinates(office.latitude + 0.0002, office.longitude), 5f, false)

    private fun useCase(location: FakeLocationRepository) = ObserveProximityUseCase(
        FakeOfficeRepository(office), location, ProximityEvaluator(AttendancePolicy())
    )

    @Test
    fun notTracking_neverOpensLocationStream() = runTest {
        val location = FakeLocationRepository(stream = flow { error("must not be collected") })
        assertEquals(Proximity.Searching, useCase(location)(tracking = false).first())
    }

    @Test
    fun tracking_emitsMeasuredProximity() = runTest {
        val location = FakeLocationRepository(stream = flowOf(nearby))
        val measured = useCase(location)(tracking = true).first { it is Proximity.Measured }
        assertTrue((measured as Proximity.Measured).canCheckIn)
    }

    @Test
    fun streamFailure_fallsBackToSearching() = runTest {
        val location = FakeLocationRepository(stream = flow { throw SecurityException("revoked") })
        assertEquals(Proximity.Searching, useCase(location)(tracking = true).first())
    }
}
