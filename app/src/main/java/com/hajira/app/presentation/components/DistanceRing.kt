package com.hajira.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hajira.app.presentation.theme.HajiraColors

enum class RangeTone(val color: Color, val soft: Color) {
    Idle(HajiraColors.Muted, HajiraColors.Track),
    InRange(HajiraColors.Green, HajiraColors.GreenSoft),
    OutOfRange(HajiraColors.Red, HajiraColors.RedSoft),
    Weak(HajiraColors.Amber, HajiraColors.AmberSoft)
}

private const val RING_FULL_AT_METERS = 300f

@Composable
fun DistanceRing(
    distanceMeters: Float?,
    tone: RangeTone,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 150.dp
) {
    val target = when {
        distanceMeters == null -> 0f
        tone == RangeTone.InRange -> 1f
        else -> (distanceMeters / RING_FULL_AT_METERS).coerceIn(0.05f, 1f)
    }
    val progress by animateFloatAsState(target, tween(600), label = "ringProgress")
    val ringColor by animateColorAsState(tone.color, label = "ringColor")

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = HajiraColors.Track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(Modifier.padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                fontSize = if (diameter < 130.dp) 24.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = HajiraColors.Ink,
                maxLines = 1,
                softWrap = false
            )
            Text(caption, fontSize = 10.sp, color = HajiraColors.Muted, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun StatusChip(text: String, tone: RangeTone) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(tone.soft)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tone.color))
        Text(text, color = tone.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}
