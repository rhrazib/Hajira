package com.hajira.app.presentation

import android.content.Context
import com.hajira.app.R
import java.util.Locale
import kotlin.math.roundToInt

fun formatDistance(meters: Float): String =
    if (meters >= 1000f) String.format(Locale.US, "%.1fkm", meters / 1000f)
    else "${meters.roundToInt()}m"

fun AttendanceEvent.toMessage(context: Context): String = when (this) {
    AttendanceEvent.OfficeSaved -> context.getString(R.string.msg_office_saved)
    AttendanceEvent.NoFix -> context.getString(R.string.msg_no_fix)
    is AttendanceEvent.WeakSignal -> context.getString(R.string.msg_weak_signal, accuracyMeters)
    AttendanceEvent.MockBlocked -> context.getString(R.string.msg_mock_blocked)
    AttendanceEvent.PermissionMissing -> context.getString(R.string.msg_permission_missing)
    AttendanceEvent.Failure -> context.getString(R.string.msg_failure)
    AttendanceEvent.CheckedIn -> context.getString(R.string.msg_checked_in)
    is AttendanceEvent.OutOfRange -> context.getString(R.string.msg_out_of_range, formatDistance(distanceMeters))
    AttendanceEvent.WindowClosed -> context.getString(R.string.msg_window_closed)
    AttendanceEvent.AlreadyMarked -> context.getString(R.string.msg_already_marked)
    AttendanceEvent.OfficeNotSet -> context.getString(R.string.msg_office_not_set)
    AttendanceEvent.CheckInReset -> context.getString(R.string.msg_check_in_reset)
}
