package com.hajira.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.domain.model.LocationFix
import com.hajira.app.domain.repository.LocationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Callers must make sure ACCESS_FINE_LOCATION is granted before collecting / calling. */
@SuppressLint("MissingPermission")
class FusedLocationRepository(context: Context) : LocationRepository {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    override fun updates(): Flow<LocationFix> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toFix()) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { close(it) }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    override suspend fun currentFix(): LocationFix? {
        val token = CancellationTokenSource()
        return client
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .await()
            ?.toFix()
    }

    @Suppress("DEPRECATION")
    private fun Location.toFix(): LocationFix {
        val mocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider
        return LocationFix(Coordinates(latitude, longitude), accuracy, mocked)
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 2_000L
        const val MIN_UPDATE_INTERVAL_MS = 1_000L
    }
}
