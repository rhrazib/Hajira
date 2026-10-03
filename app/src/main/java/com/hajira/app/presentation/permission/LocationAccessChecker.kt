package com.hajira.app.presentation.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.hajira.app.domain.model.LocationAccess

/** Android-specific glue: turns permission + GPS switch state into a domain [LocationAccess]. */
object LocationAccessChecker {

    private const val FINE = Manifest.permission.ACCESS_FINE_LOCATION
    private const val COARSE = Manifest.permission.ACCESS_COARSE_LOCATION

    /** Ask for both; on Android 12+ the user may still pick "Approximate" only. */
    val PERMISSIONS: Array<String> = arrayOf(FINE, COARSE)

    fun check(context: Context, askedBefore: Boolean): LocationAccess {
        if (!context.isGranted(FINE)) {
            if (context.isGranted(COARSE)) return LocationAccess.APPROXIMATE_ONLY

            val canAskAgain = context.findActivity()?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, FINE)
            } ?: true
            return if (askedBefore && !canAskAgain) {
                LocationAccess.PERMANENTLY_DENIED
            } else {
                LocationAccess.DENIED
            }
        }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (LocationManagerCompat.isLocationEnabled(manager)) {
            LocationAccess.READY
        } else {
            LocationAccess.GPS_OFF
        }
    }

    private fun Context.isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
