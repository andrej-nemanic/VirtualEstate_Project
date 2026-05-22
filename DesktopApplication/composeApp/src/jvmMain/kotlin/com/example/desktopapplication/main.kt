package com.example.desktopapplication

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Real Estate Database Manager",
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        AppNavigation()
    }
}
