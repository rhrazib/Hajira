package com.hajira.app.domain.usecase

import com.hajira.app.domain.model.SetOfficeResult
import com.hajira.app.domain.policy.AttendancePolicy
import com.hajira.app.domain.repository.LocationRepository
import com.hajira.app.domain.repository.OfficeRepository
import kotlin.coroutines.cancellation.CancellationException

class SetOfficeLocationUseCase(
    private val locationRepository: LocationRepository,
    private val officeRepository: OfficeRepository,
    private val policy: AttendancePolicy
) {
    suspend operator fun invoke(): SetOfficeResult {
        val fix = when (val outcome = locationRepository.fetchFix()) {
            is FixOutcome.Got -> outcome.fix
            FixOutcome.None -> return SetOfficeResult.NoFix
            FixOutcome.NoPermission -> return SetOfficeResult.PermissionMissing
            FixOutcome.Error -> return SetOfficeResult.Failed
        }
        if (fix.isMock && policy.blockMockLocation) return SetOfficeResult.MockBlocked
        if (!policy.isAccurateEnough(fix.accuracyMeters)) {
            return SetOfficeResult.WeakSignal(fix.accuracyMeters)
        }
        return try {
            officeRepository.save(fix.coordinates)
            SetOfficeResult.Saved(fix.coordinates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SetOfficeResult.Failed
        }
    }
}
