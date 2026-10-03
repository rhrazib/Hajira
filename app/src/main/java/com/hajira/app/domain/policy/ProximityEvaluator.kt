package com.hajira.app.domain.policy

import com.hajira.app.domain.geo.GeoMath
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.model.Proximity

/** Pure function of (office, fix, policy) -> Proximity. No Android, no coroutines. */
class ProximityEvaluator(private val policy: AttendancePolicy) {

    fun evaluate(office: Coordinates?, fix: LocationFix?): Proximity {
        if (office == null) return Proximity.OfficeNotSet
        if (fix == null) return Proximity.Searching
        if (fix.isMock && policy.blockMockLocation) return Proximity.MockBlocked

        val distance = GeoMath.distanceMeters(fix.coordinates, office)
        return Proximity.Measured(
            distanceMeters = distance,
            accuracyMeters = fix.accuracyMeters,
            withinRadius = policy.isWithinRadius(distance),
            accurateEnough = policy.isAccurateEnough(fix.accuracyMeters)
        )
    }
}
