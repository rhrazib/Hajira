package com.hajira.app.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hajira.app.data.local.hajiraDataStore
import com.hajira.app.data.location.FusedLocationRepository
import com.hajira.app.data.repository.AttendanceRepositoryImpl
import com.hajira.app.data.repository.OfficeRepositoryImpl
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.policy.ProximityEvaluator
import com.hajira.app.domain.repository.AttendanceRepository
import com.hajira.app.domain.repository.LocationRepository
import com.hajira.app.domain.repository.OfficeRepository
import com.hajira.app.domain.usecase.MarkAttendanceUseCase
import com.hajira.app.domain.usecase.ObserveAttendanceStatusUseCase
import com.hajira.app.domain.usecase.ObserveOfficeUseCase
import com.hajira.app.domain.usecase.ObserveProximityUseCase
import com.hajira.app.domain.usecase.ResetCheckInUseCase
import com.hajira.app.domain.usecase.SetOfficeLocationUseCase
import com.hajira.app.presentation.AttendanceViewModel
import java.time.Clock

/**
 * Hand-written composition root. The graph is tiny, so a DI framework would be ceremony;
 * swapping this for Hilt later only touches this file and the Application/Activity.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val policy = AttendancePolicy()
    private val clock: Clock = Clock.systemDefaultZone()
    private val evaluator = ProximityEvaluator(policy)

    private val dataStore = appContext.hajiraDataStore
    private val officeRepository: OfficeRepository = OfficeRepositoryImpl(dataStore)
    private val attendanceRepository: AttendanceRepository = AttendanceRepositoryImpl(dataStore)
    private val locationRepository: LocationRepository = FusedLocationRepository(appContext)

    fun attendanceViewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            AttendanceViewModel(
                policy = policy,
                observeOffice = ObserveOfficeUseCase(officeRepository),
                observeProximity = ObserveProximityUseCase(officeRepository, locationRepository, evaluator),
                observeAttendanceStatus = ObserveAttendanceStatusUseCase(attendanceRepository, policy, clock),
                setOfficeLocation = SetOfficeLocationUseCase(locationRepository, officeRepository, policy),
                markAttendance = MarkAttendanceUseCase(
                    attendanceRepository, officeRepository, locationRepository, evaluator, policy, clock
                ),
                resetCheckIn = ResetCheckInUseCase(attendanceRepository)
            )
        }
    }
}
