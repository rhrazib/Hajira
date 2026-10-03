package com.hajira.app.domain.model

sealed interface SetOfficeResult {
    data class Saved(val coordinates: Coordinates) : SetOfficeResult
    data object NoFix : SetOfficeResult
    data class WeakSignal(val accuracyMeters: Float) : SetOfficeResult
    data object MockBlocked : SetOfficeResult
    data object PermissionMissing : SetOfficeResult
    data object Failed : SetOfficeResult
}

sealed interface MarkAttendanceResult {
    data class CheckedIn(val atMillis: Long) : MarkAttendanceResult
    data class OutOfRange(val distanceMeters: Float) : MarkAttendanceResult
    data class WeakSignal(val accuracyMeters: Float) : MarkAttendanceResult
    data object MockBlocked : MarkAttendanceResult
    data object WindowClosed : MarkAttendanceResult
    data object AlreadyMarked : MarkAttendanceResult
    data object OfficeNotSet : MarkAttendanceResult
    data object NoFix : MarkAttendanceResult
    data object PermissionMissing : MarkAttendanceResult
    data object Failed : MarkAttendanceResult
}
