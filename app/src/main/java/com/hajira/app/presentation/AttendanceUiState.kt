package com.hajira.app.presentation

import com.hajira.app.domain.model.AttendanceStatus
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationAccess
import com.hajira.app.domain.model.Proximity
import com.hajira.app.domain.policy.AttendancePolicy

/** Everything the screen renders. Derived data lives here as computed properties. */
data class AttendanceUiState(
    val policy: AttendancePolicy = AttendancePolicy(),
    val office: Coordinates? = null,
    val access: LocationAccess = LocationAccess.UNKNOWN,
    val proximity: Proximity = Proximity.OfficeNotSet,
    val attendance: AttendanceStatus = AttendanceStatus(),
    val isSavingOffice: Boolean = false,
    val isMarking: Boolean = false
) {
    val isCheckedIn: Boolean get() = attendance.checkedInAtMillis != null

    val canSetOffice: Boolean
        get() = access == LocationAccess.READY && !isSavingOffice && !isMarking

    val canMark: Boolean
        get() = access == LocationAccess.READY &&
            attendance.windowOpen &&
            !isCheckedIn &&
            !isMarking &&
            (proximity as? Proximity.Measured)?.canCheckIn == true
}

/** One-shot messages (snackbar). Kept out of [AttendanceUiState] so they cannot replay. */
sealed interface AttendanceEvent {
    data object OfficeSaved : AttendanceEvent
    data object NoFix : AttendanceEvent
    data class WeakSignal(val accuracyMeters: Int) : AttendanceEvent
    data object MockBlocked : AttendanceEvent
    data object PermissionMissing : AttendanceEvent
    data object Failure : AttendanceEvent
    data object CheckedIn : AttendanceEvent
    data class OutOfRange(val distanceMeters: Float) : AttendanceEvent
    data object WindowClosed : AttendanceEvent
    data object AlreadyMarked : AttendanceEvent
    data object OfficeNotSet : AttendanceEvent
    data object CheckInReset : AttendanceEvent
}
