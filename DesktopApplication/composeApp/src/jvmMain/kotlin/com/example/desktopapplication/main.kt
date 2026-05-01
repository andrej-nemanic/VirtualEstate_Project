package com.example.desktopapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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