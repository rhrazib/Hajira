package com.hajira.app.domain

import com.hajira.app.domain.geo.GeoMath
import com.hajira.app.domain.model.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    private val office = Coordinates(23.8103, 90.4125)

    @Test
    fun samePoint_isZero() {
        assertEquals(0f, GeoMath.distanceMeters(office, office), 0.01f)
    }

    @Test
    fun oneThousandthOfDegreeLatitude_isAbout111m() {
        val other = Coordinates(office.latitude + 0.001, office.longitude)
        assertEquals(111.2f, GeoMath.distanceMeters(office, other), 0.5f)
    }

    @Test
    fun distanceIsSymmetric() {
        val other = Coordinates(23.8150, 90.4200)
        assertEquals(
            GeoMath.distanceMeters(office, other),
            GeoMath.distanceMeters(other, office),
            0.001f
        )
    }
}
