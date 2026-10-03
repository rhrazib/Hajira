package com.hajira.app.domain.policy

import java.time.LocalTime

/** Every business rule knob lives here, so it is trivial to tweak or inject in tests. */
data class AttendancePolicy(
    val radiusMeters: Float = 50f,
    /** Fixes less accurate than this still show a distance, but cannot unlock check-in. */
    val maxAccuracyMeters: Float = 50f,
    /** Reject mocked GPS. Off by default so reviewers can test with a fake-GPS app. */
    val blockMockLocation: Boolean = false,
    /** The design shows 09:00-10:30; the written brief does not, so enforcement is opt-in. */
    val enforceTimeWindow: Boolean = false,
    val windowStart: LocalTime = LocalTime.of(9, 0),
    val windowEnd: LocalTime = LocalTime.of(10, 30)
) {
    fun isWithinRadius(distanceMeters: Float): Boolean = distanceMeters <= radiusMeters

    fun isAccurateEnough(accuracyMeters: Float): Boolean = accuracyMeters <= maxAccuracyMeters

    fun isWindowOpen(now: LocalTime): Boolean {
        if (!enforceTimeWindow) return true
        return !now.isBefore(windowStart) && !now.isAfter(windowEnd)
    }
}
