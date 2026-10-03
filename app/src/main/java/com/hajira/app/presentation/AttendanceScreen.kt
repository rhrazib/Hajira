package com.hajira.app.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hajira.app.R
import com.hajira.app.domain.model.LocationAccess
import com.hajira.app.domain.model.Proximity
import com.hajira.app.presentation.components.DistanceRing
import com.hajira.app.presentation.components.OfficeMap
import com.hajira.app.presentation.components.RangeTone
import com.hajira.app.presentation.components.StatusChip
import com.hajira.app.presentation.permission.LocationAccessChecker
import com.hajira.app.presentation.theme.HajiraColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

/** What the screen can ask the outside world to do. Keeps [AttendanceContent] stateless. */
class AttendanceActions(
    val onBack: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenLocationSettings: () -> Unit,
    val onSetOffice: () -> Unit,
    val onMarkAttendance: () -> Unit,
    val onResetCheckIn: () -> Unit
)

/** The AttendanceScreen: stateful entry point. Owns permission launching, lifecycle hooks and one-shot events. */
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    fun refreshAccess() {
        viewModel.onAccessChanged(LocationAccessChecker.check(context, askedOnce))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        askedOnce = true
        refreshAccess()
    }

    // Covers first launch, coming back from Settings, and the GPS toggle changing.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshAccess() }

    LaunchedEffect(state.access) {
        if (state.access == LocationAccess.DENIED && !askedOnce) {
            permissionLauncher.launch(LocationAccessChecker.PERMISSIONS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(event.toMessage(context)) }
        }
    }

    val actions = AttendanceActions(
        onBack = onBack,
        onRequestPermission = { permissionLauncher.launch(LocationAccessChecker.PERMISSIONS) },
        onOpenAppSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            )
        },
        onOpenLocationSettings = {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        onSetOffice = viewModel::onSetOfficeClick,
        onMarkAttendance = viewModel::onMarkAttendanceClick,
        onResetCheckIn = viewModel::onResetCheckInClick
    )

    AttendanceContent(state = state, snackbar = snackbar, actions = actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceContent(
    state: AttendanceUiState,
    snackbar: SnackbarHostState,
    actions: AttendanceActions
) {
    Scaffold(
        containerColor = HajiraColors.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.title_attendance),
                        color = HajiraColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        // Capped + centred width for tablets; sizes shrink on short screens so the whole
        // screen fits without scrolling. Scrolling stays as a fallback (tiny screens, big fonts).
        BoxWithConstraints(
            Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val dimens = Dimens.forHeight(maxHeight)
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = dimens.gap),
                verticalArrangement = Arrangement.spacedBy(dimens.gap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AccessBanner(state.access, actions)
                OfficeCard(state, dimens, actions.onSetOffice)
                RangeStatus(state, dimens)
                CheckInCard(state, dimens, actions)
            }
        }
    }
}

/** Layout sizes derived from the height available below the app bar. */
private class Dimens(
    val map: Dp,
    val ring: Dp,
    val gap: Dp,
    val inner: Dp
) {
    companion object {
        fun forHeight(available: Dp) = Dimens(
            map = (available * 0.15f).coerceIn(92.dp, 150.dp),
            ring = (available * 0.16f).coerceIn(104.dp, 150.dp),
            gap = if (available < 720.dp) 10.dp else 16.dp,
            inner = if (available < 720.dp) 10.dp else 14.dp
        )
    }
}

private class BannerSpec(
    @StringRes val text: Int,
    @StringRes val action: Int,
    val onClick: () -> Unit
)

@Composable
private fun AccessBanner(access: LocationAccess, actions: AttendanceActions) {
    val spec = when (access) {
        LocationAccess.DENIED ->
            BannerSpec(R.string.banner_denied, R.string.action_allow, actions.onRequestPermission)
        LocationAccess.PERMANENTLY_DENIED ->
            BannerSpec(R.string.banner_blocked, R.string.action_open_settings, actions.onOpenAppSettings)
        LocationAccess.APPROXIMATE_ONLY ->
            BannerSpec(R.string.banner_approximate, R.string.action_open_settings, actions.onOpenAppSettings)
        LocationAccess.GPS_OFF ->
            BannerSpec(R.string.banner_gps_off, R.string.action_turn_on, actions.onOpenLocationSettings)
        LocationAccess.UNKNOWN, LocationAccess.READY -> return
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HajiraColors.RedSoft)
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(spec.text),
                Modifier.weight(1f),
                fontSize = 13.sp,
                color = HajiraColors.Ink
            )
            TextButton(onClick = spec.onClick) { Text(stringResource(spec.action)) }
        }
    }
}

@Composable
private fun OfficeCard(state: AttendanceUiState, dimens: Dimens, onSetOffice: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(dimens.inner)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.step_office_context),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HajiraColors.Muted,
                    letterSpacing = 0.8.sp
                )
                Box(Modifier.size(7.dp).background(HajiraColors.Primary, CircleShape))
            }

            OfficeMap(
                office = state.office,
                radiusMeters = state.policy.radiusMeters,
                height = dimens.map
            )

            Text(
                stringResource(R.string.office_hint),
                fontSize = 13.sp,
                color = HajiraColors.Muted
            )

            OutlinedButton(
                onClick = onSetOffice,
                enabled = state.canSetOffice,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, HajiraColors.Primary)
            ) {
                if (state.isSavingOffice) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.set_office_location), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RangeStatus(state: AttendanceUiState, dimens: Dimens) {
    val proximity = state.proximity
    val measured = proximity as? Proximity.Measured
    val radius = state.policy.radiusMeters.roundToInt()

    val value = measured?.let { formatDistance(it.distanceMeters) } ?: "--"
    val caption = stringResource(
        when {
            proximity is Proximity.OfficeNotSet -> R.string.caption_no_office
            measured == null -> R.string.caption_searching
            else -> R.string.caption_away
        }
    )

    val tone = when {
        measured == null -> RangeTone.Idle
        !measured.accurateEnough -> RangeTone.Weak
        measured.withinRadius -> RangeTone.InRange
        else -> RangeTone.OutOfRange
    }

    val chip = stringResource(
        when {
            proximity is Proximity.OfficeNotSet -> R.string.chip_office_not_set
            proximity is Proximity.MockBlocked -> R.string.chip_mock_blocked
            measured == null -> R.string.chip_waiting_gps
            !measured.accurateEnough -> R.string.chip_weak_signal
            measured.withinRadius -> R.string.chip_in_range
            else -> R.string.chip_out_of_range
        }
    )

    val hint = when {
        proximity is Proximity.OfficeNotSet -> stringResource(R.string.hint_set_office)
        proximity is Proximity.MockBlocked -> stringResource(R.string.hint_mock_blocked)
        measured == null -> stringResource(
            if (state.access == LocationAccess.READY) R.string.hint_getting_location
            else R.string.hint_need_access
        )
        !measured.accurateEnough ->
            stringResource(R.string.hint_weak_signal, measured.accuracyMeters.roundToInt())
        measured.withinRadius -> stringResource(R.string.hint_in_zone, radius)
        else -> stringResource(R.string.hint_out_of_range, formatDistance(measured.distanceMeters), radius)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DistanceRing(measured?.distanceMeters, tone, value, caption, diameter = dimens.ring)
        StatusChip(chip, tone)
        Text(
            hint,
            fontSize = 12.sp,
            color = HajiraColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun CheckInCard(state: AttendanceUiState, dimens: Dimens, actions: AttendanceActions) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
    val checkedInAt = state.attendance.checkedInAtMillis

    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFD1D5DB),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = dash)
                )
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.inner - 2.dp)
    ) {
        when {
            checkedInAt != null -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = HajiraColors.Green,
                modifier = Modifier.size(32.dp)
            )
            // unlocked while check-in is available, as the design only shows a lock when it is not
            state.canMark -> Icon(
                painterResource(R.drawable.ic_lock_open),
                contentDescription = null,
                tint = HajiraColors.Primary,
                modifier = Modifier.size(32.dp)
            )
            else -> Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = HajiraColors.Muted,
                modifier = Modifier.size(32.dp)
            )
        }

        Button(
            onClick = actions.onMarkAttendance,
            enabled = state.canMark,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HajiraColors.Primary,
                disabledContainerColor = HajiraColors.Locked,
                disabledContentColor = HajiraColors.Muted
            )
        ) {
            if (state.isMarking) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(
                    stringResource(
                        if (checkedInAt != null) R.string.btn_attendance_marked else R.string.btn_mark_attendance
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val footer = when {
            checkedInAt != null -> stringResource(R.string.footer_checked_in_at, formatTime(checkedInAt))
            state.policy.enforceTimeWindow -> stringResource(
                R.string.footer_window,
                state.policy.windowStart.format(timeFormat),
                state.policy.windowEnd.format(timeFormat)
            )
            else -> null
        }
        footer?.let {
            Text(it, fontSize = 10.sp, color = HajiraColors.Muted, letterSpacing = 1.sp)
        }

        if (checkedInAt != null) {
            TextButton(onClick = actions.onResetCheckIn) {
                Text(stringResource(R.string.btn_reset_demo), fontSize = 12.sp)
            }
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormat)
