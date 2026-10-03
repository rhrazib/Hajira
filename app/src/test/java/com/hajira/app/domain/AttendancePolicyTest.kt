package com.hajira.app.domain

import com.hajira.app.domain.policy.AttendancePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class AttendancePolicyTest {

    private val policy = AttendancePolicy()

    @Test
    fun radius_isInclusiveAtFiftyMeters() {
        assertTrue(policy.isWithinRadius(50f))
        assertFalse(policy.isWithinRadius(50.1f))
    }

    @Test
    fun accuracy_gate() {
        assertTrue(policy.isAccurateEnough(10f))
        assertFalse(policy.isAccurateEnough(80f))
    }

    @Test
    fun timeWindow_isIgnoredByDefault() {
        assertTrue(policy.isWindowOpen(LocalTime.of(3, 0)))
    }

    @Test
    fun timeWindow_whenEnforced_isInclusive() {
        val strict = AttendancePolicy(enforceTimeWindow = true)
        assertFalse(strict.isWindowOpen(LocalTime.of(8, 59)))
        assertTrue(strict.isWindowOpen(LocalTime.of(9, 0)))
        assertTrue(strict.isWindowOpen(LocalTime.of(10, 30)))
        assertFalse(strict.isWindowOpen(LocalTime.of(10, 31)))
    }
}
