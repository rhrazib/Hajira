package com.hajira.app.domain

import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.model.Proximity
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.policy.ProximityEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityEvaluatorTest {

    private val office = Coordinates(23.8103, 90.4125)
    private val evaluator = ProximityEvaluator(AttendancePolicy())

    private fun fixAt(deltaLat: Double, accuracy: Float = 5f, mock: Boolean = false) =
        LocationFix(Coordinates(office.latitude + deltaLat, office.longitude), accuracy, mock)

    @Test
    fun noOffice_isOfficeNotSet() {
        assertEquals(Proximity.OfficeNotSet, evaluator.evaluate(null, fixAt(0.0)))
    }

    @Test
    fun noFix_isSearching() {
        assertEquals(Proximity.Searching, evaluator.evaluate(office, null))
    }

    @Test
    fun about44m_isInRange() {
        val result = evaluator.evaluate(office, fixAt(0.0004)) as Proximity.Measured
        assertTrue(result.withinRadius)
        assertTrue(result.canCheckIn)
    }

    @Test
    fun about56m_isOutOfRange() {
        val result = evaluator.evaluate(office, fixAt(0.0005)) as Proximity.Measured
        assertFalse(result.withinRadius)
        assertFalse(result.canCheckIn)
    }

    @Test
    fun weakAccuracy_keepsDistanceButBlocksCheckIn() {
        val result = evaluator.evaluate(office, fixAt(0.0001, accuracy = 80f)) as Proximity.Measured
        assertTrue(result.withinRadius)
        assertFalse(result.accurateEnough)
        assertFalse(result.canCheckIn)
    }

    @Test
    fun mockLocation_blockedOnlyWhenPolicySaysSo() {
        val mocked = fixAt(0.0, mock = true)
        assertTrue(evaluator.evaluate(office, mocked) is Proximity.Measured)

        val strict = ProximityEvaluator(AttendancePolicy(blockMockLocation = true))
        assertEquals(Proximity.MockBlocked, strict.evaluate(office, mocked))
    }
}
