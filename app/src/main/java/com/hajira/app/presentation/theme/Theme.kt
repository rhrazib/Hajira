package com.hajira.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HajiraColors {
    val Primary = Color(0xFF2347B5)
    val Background = Color(0xFFF3F7F8)
    val Ink = Color(0xFF1F2937)
    val Muted = Color(0xFF6B7280)
    val Track = Color(0xFFE5E7EB)
    val Red = Color(0xFFEF4444)
    val RedSoft = Color(0xFFFEE9E9)
    val Green = Color(0xFF16A34A)
    val GreenSoft = Color(0xFFDCFCE7)
    val Amber = Color(0xFFB45309)
    val AmberSoft = Color(0xFFFEF3C7)
    val Locked = Color(0xFFC3CCDD)
}

@Composable
fun HajiraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = HajiraColors.Primary),
        content = content
    )
}
