package com.hajira.app.domain.model

data class Coordinates(val latitude: Double, val longitude: Double)

data class LocationFix(
    val coordinates: Coordinates,
    val accuracyMeters: Float,
    val isMock: Boolean
)

/** What the app is currently allowed to do with the device's location. */
enum class LocationAccess { UNKNOWN, DENIED, PERMANENTLY_DENIED, APPROXIMATE_ONLY, GPS_OFF, READY }

/** Where the user stands relative to the saved office. */
sealed interface Proximity {
    data object OfficeNotSet : Proximity

    /** Office is saved but there is no usable fix yet. */
    data object Searching : Proximity

    data object MockBlocked : Proximity

    data class Measured(
        val distanceMeters: Float,
        val accuracyMeters: Float,
        val withinRadius: Boolean,
        val accurateEnough: Boolean
    ) : Proximity {
        val canCheckIn: Boolean get() = withinRadius && accurateEnough
    }
}

data class AttendanceStatus(
    val checkedInAtMillis: Long? = null,
    val windowOpen: Boolean = true
)
