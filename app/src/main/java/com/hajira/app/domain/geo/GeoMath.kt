package com.hajira.app.domain.geo

import com.hajira.app.domain.model.Coordinates
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Plain haversine, so it can be unit tested without the Android framework. */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceMeters(from: Coordinates, to: Coordinates): Float {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return (2 * EARTH_RADIUS_M * asin(sqrt(a))).toFloat()
    }
}
