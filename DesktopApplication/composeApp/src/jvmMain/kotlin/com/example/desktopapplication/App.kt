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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
/**
 * Enumeration of all possible screens in the application.
 */
enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Pregled podatkov", Icons.Default.List),
    Management("Upravljanje (CRUD)", Icons.Default.Edit),
    WebSources("Spletni viri", Icons.Default.CloudDownload),
    Generator("Generator podatkov", Icons.Default.Build)
}
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Stranska vrstica (Sidebar)
        Column(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight()
                .background(Color(0xFF2C3E50)) // Temnejša barva za profesionalen videz
                .padding(vertical = 16.dp)
        ) {
            Text(
                "Upravljalec baze",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigacijski elementi
            Screen.values().forEach { screen ->
                NavigationItem(
                    screen = screen,
                    isSelected = currentScreen == screen,
                    onClick = { currentScreen = screen }
                )
            }
        }

        // Osrednji del za vsebino
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(24.dp)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> Column {
                    Text("Pregled vseh nepremičnin", style = MaterialTheme.typography.h4)
                    Text("Tukaj bodo prikazani podatki iz API-ja.")
                }
                Screen.Management -> Column {
                    Text("Vnos in urejanje", style = MaterialTheme.typography.h4)
                    Text("Obrazci za vnos podatkov v tabelo Nepremicnina.")
                }
                Screen.WebSources -> Column {
                    Text("Podatki iz spletnih virov", style = MaterialTheme.typography.h4)
                    Text("Razčlenjevanje in filtriranje zunanjih virov.")
                }
                Screen.Generator -> Column {
                    Text("Generator namišljenih podatkov", style = MaterialTheme.typography.h4)
                    Text("Uporaba kotlin-faker za generiranje vsebine[cite: 4].")
                }
            }
        }
    }
}

@Composable
fun NavigationItem(screen: Screen, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) Color(0xFF34495E) else Color.Transparent
    val contentColor = if (isSelected) Color.Cyan else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = screen.title,
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}