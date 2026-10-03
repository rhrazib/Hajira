package com.hajira.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hajira.app.domain.model.LocationAccess
import com.hajira.app.domain.model.MarkAttendanceResult
import com.hajira.app.domain.model.SetOfficeResult
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.usecase.MarkAttendanceUseCase
import com.hajira.app.domain.usecase.ObserveAttendanceStatusUseCase
import com.hajira.app.domain.usecase.ObserveOfficeUseCase
import com.hajira.app.domain.usecase.ObserveProximityUseCase
import com.hajira.app.domain.usecase.ResetCheckInUseCase
import com.hajira.app.domain.usecase.SetOfficeLocationUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AttendanceViewModel(
    private val policy: AttendancePolicy,
    observeOffice: ObserveOfficeUseCase,
    observeProximity: ObserveProximityUseCase,
    observeAttendanceStatus: ObserveAttendanceStatusUseCase,
    private val setOfficeLocation: SetOfficeLocationUseCase,
    private val markAttendance: MarkAttendanceUseCase,
    private val resetCheckIn: ResetCheckInUseCase
) : ViewModel() {

    private data class Busy(val savingOffice: Boolean = false, val marking: Boolean = false)

    private val access = MutableStateFlow(LocationAccess.UNKNOWN)
    private val busy = MutableStateFlow(Busy())

    private val _events = Channel<AttendanceEvent>(Channel.BUFFERED)
    val events: Flow<AttendanceEvent> = _events.receiveAsFlow()

    // The GPS stream is only open while we are actually allowed to use it.
    private val proximity = access
        .map { it == LocationAccess.READY }
        .distinctUntilChanged()
        .flatMapLatest { tracking -> observeProximity(tracking) }

    val uiState: StateFlow<AttendanceUiState> = combine(
        observeOffice(),
        proximity,
        observeAttendanceStatus(),
        access,
        busy
    ) { savedOffice, currentProximity, currentAttendance, currentAccess, currentBusy ->
        AttendanceUiState(
            policy = policy,
            office = savedOffice,
            access = currentAccess,
            proximity = currentProximity,
            attendance = currentAttendance,
            isSavingOffice = currentBusy.savingOffice,
            isMarking = currentBusy.marking
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AttendanceUiState(policy = policy))

    fun onAccessChanged(newAccess: LocationAccess) {
        access.value = newAccess
    }

    fun onSetOfficeClick() {
        if (uiState.value.access != LocationAccess.READY) {
            _events.trySend(AttendanceEvent.PermissionMissing)
            return
        }
        if (busy.value.savingOffice) return
        viewModelScope.launch {
            busy.update { it.copy(savingOffice = true) }
            val result = try {
                setOfficeLocation()
            } finally {
                busy.update { it.copy(savingOffice = false) }
            }
            _events.send(result.toEvent())
        }
    }

    fun onMarkAttendanceClick() {
        if (busy.value.marking) return
        viewModelScope.launch {
            busy.update { it.copy(marking = true) }
            val result = try {
                markAttendance()
            } finally {
                busy.update { it.copy(marking = false) }
            }
            _events.send(result.toEvent())
        }
    }

    fun onResetCheckInClick() {
        viewModelScope.launch {
            resetCheckIn()
            _events.send(AttendanceEvent.CheckInReset)
        }
    }

    private fun SetOfficeResult.toEvent(): AttendanceEvent = when (this) {
        is SetOfficeResult.Saved -> AttendanceEvent.OfficeSaved
        SetOfficeResult.NoFix -> AttendanceEvent.NoFix
        is SetOfficeResult.WeakSignal -> AttendanceEvent.WeakSignal(accuracyMeters.roundToInt())
        SetOfficeResult.MockBlocked -> AttendanceEvent.MockBlocked
        SetOfficeResult.PermissionMissing -> AttendanceEvent.PermissionMissing
        SetOfficeResult.Failed -> AttendanceEvent.Failure
    }

    private fun MarkAttendanceResult.toEvent(): AttendanceEvent = when (this) {
        is MarkAttendanceResult.CheckedIn -> AttendanceEvent.CheckedIn
        is MarkAttendanceResult.OutOfRange -> AttendanceEvent.OutOfRange(distanceMeters)
        is MarkAttendanceResult.WeakSignal -> AttendanceEvent.WeakSignal(accuracyMeters.roundToInt())
        MarkAttendanceResult.MockBlocked -> AttendanceEvent.MockBlocked
        MarkAttendanceResult.WindowClosed -> AttendanceEvent.WindowClosed
        MarkAttendanceResult.AlreadyMarked -> AttendanceEvent.AlreadyMarked
        MarkAttendanceResult.OfficeNotSet -> AttendanceEvent.OfficeNotSet
        MarkAttendanceResult.NoFix -> AttendanceEvent.NoFix
        MarkAttendanceResult.PermissionMissing -> AttendanceEvent.PermissionMissing
        MarkAttendanceResult.Failed -> AttendanceEvent.Failure
    }
}
