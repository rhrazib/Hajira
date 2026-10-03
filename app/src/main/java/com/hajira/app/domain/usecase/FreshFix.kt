package com.hajira.app.domain.usecase

import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.repository.LocationRepository
import kotlin.coroutines.cancellation.CancellationException

internal sealed interface FixOutcome {
    data class Got(val fix: LocationFix) : FixOutcome
    data object None : FixOutcome
    data object NoPermission : FixOutcome
    data object Error : FixOutcome
}

/** Reads one fresh fix and turns every failure mode into a value instead of an exception. */
internal suspend fun LocationRepository.fetchFix(): FixOutcome = try {
    currentFix()?.let { FixOutcome.Got(it) } ?: FixOutcome.None
} catch (e: CancellationException) {
    throw e
} catch (e: SecurityException) {
    FixOutcome.NoPermission
} catch (e: Exception) {
    FixOutcome.Error
}
