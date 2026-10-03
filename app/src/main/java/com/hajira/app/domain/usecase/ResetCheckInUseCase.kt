package com.hajira.app.domain.usecase

import com.hajira.app.domain.repository.AttendanceRepository

/** Demo helper so a reviewer can try the flow again on the same day. */
class ResetCheckInUseCase(private val attendanceRepository: AttendanceRepository) {
    suspend operator fun invoke() = attendanceRepository.clear()
}
