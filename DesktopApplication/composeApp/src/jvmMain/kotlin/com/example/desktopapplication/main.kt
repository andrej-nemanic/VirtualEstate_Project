package com.example.desktopapplication

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowIcon = rememberVectorPainter(image = Icons.Default.Home)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Real Estate Database Manager",
        icon = windowIcon,
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        AppNavigation()
    }
}
