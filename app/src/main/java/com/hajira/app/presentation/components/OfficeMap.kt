package com.hajira.app.presentation.components

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hajira.app.R
import com.hajira.app.domain.model.Coordinates
import com.hajira.app.presentation.theme.HajiraColors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.util.Locale

private const val OFFICE_ZOOM = 17.5
private const val FALLBACK_ZOOM = 11.0
private val FALLBACK_CENTER = GeoPoint(23.8103, 90.4125)

/**
 * Read-only OpenStreetMap preview of the saved office with the geofence drawn on top.
 * Needs no API key. Offline, the tiles stay blank but the coordinates pill still works.
 */
@Composable
fun OfficeMap(
    office: Coordinates?,
    radiusMeters: Float,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { createMapView(context) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE8EEF0))
    ) {
        AndroidView(
            factory = { mapView },
            update = { it.showOffice(office, radiusMeters) },
            modifier = Modifier.fillMaxSize()
        )

        // Swallows touches so the map stays put and the page can still scroll over it.
        Box(Modifier.matchParentSize().pointerInput(Unit) {})

        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = HajiraColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = office?.let {
                        String.format(Locale.US, "Lat: %.4f, Lon: %.4f", it.latitude, it.longitude)
                    } ?: stringResource(R.string.no_office_yet),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = HajiraColors.Ink
                )
            }
        }
    }
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
        // Keep tiles in the app cache so no storage permission is needed.
        osmdroidBasePath = File(context.cacheDir, "osmdroid")
        osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
    }
    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(false)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        isTilesScaledToDpi = true
        controller.setZoom(FALLBACK_ZOOM)
        controller.setCenter(FALLBACK_CENTER)
    }
}

private fun MapView.showOffice(office: Coordinates?, radiusMeters: Float) {
    overlays.clear()
    if (office != null) {
        val point = GeoPoint(office.latitude, office.longitude)

        val geofence = Polygon().apply {
            points = Polygon.pointsAsCircle(point, radiusMeters.toDouble())
            fillPaint.color = AndroidColor.argb(40, 35, 71, 181)
            outlinePaint.color = AndroidColor.rgb(35, 71, 181)
            outlinePaint.strokeWidth = 3f
        }
        val pin = Marker(this).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ -> true }
        }
        overlays.add(geofence)
        overlays.add(pin)

        controller.setZoom(OFFICE_ZOOM)
        controller.setCenter(point)
    }
    invalidate()
}
