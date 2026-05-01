package com.example.desktopapplication

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// Enum to define our navigation destinations


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Real Estate Database Manager",
        state = rememberWindowState(width = 1100.dp, height = 800.dp)
    ) {
        AppNavigation()
    }
}