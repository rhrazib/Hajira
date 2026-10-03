package com.hajira.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.hajira.app.presentation.AttendanceScreen
import com.hajira.app.presentation.AttendanceViewModel
import com.hajira.app.presentation.theme.HajiraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels {
        (application as HajiraApp).container.attendanceViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HajiraTheme {
                AttendanceScreen(viewModel = viewModel, onBack = { finish() })
            }
        }
    }
}
