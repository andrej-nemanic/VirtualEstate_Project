package com.example.desktopapplication

import com.example.desktopapplication.models.Property

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    color: Color = Color(0xFF2980B9),
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(45.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DataTable(
    properties: List<Property>,
    onDelete: (Int) -> Unit,
    onEdit: (Property) -> Unit
) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxSize()) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Naslov", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                Text("Mesto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Cena", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Akcije", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold)
            }
            Divider()

            // Rows
            LazyColumn {
                items(properties) { property ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(property.address, modifier = Modifier.weight(2f))
                        Text(property.city, modifier = Modifier.weight(1f))
                        Text("${property.price} €", modifier = Modifier.weight(1f))

                        Row(modifier = Modifier.weight(0.8f)) {
                            IconButton(onClick = { onEdit(property) }) {
                                Icon(Icons.Default.Edit, "Uredi", tint = Color(0xFFF39C12))
                            }
                            IconButton(onClick = { property.id?.let { onDelete(it) } }) {
                                Icon(Icons.Default.Delete, "Izbriši", tint = Color(0xFFC0392B))
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun PropertyEditDialog(
    property: Property? = null,
    onDismiss: () -> Unit,
    onConfirm: (Property) -> Unit
) {
    var address by remember { mutableStateOf(property?.address ?: "") }
    var city by remember { mutableStateOf(property?.city ?: "") }
    var price by remember { mutableStateOf(property?.price?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (property == null) "Dodaj nov zapis" else "Uredi zapis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StyledTextField(address, { address = it }, "Naslov")
                StyledTextField(city, { city = it }, "Mesto")
                StyledTextField(price, { price = it }, "Cena (€)")
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    Property(
                        id = property?.id,
                        address = address,
                        city = city,
                        type = property?.type ?: "Stanovanje",
                        size = property?.size ?: 0.0,
                        price = price.toDoubleOrNull() ?: 0.0,
                        buildYear = property?.buildYear ?: 2024
                    )
                )
            }) { Text("Shrani") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Prekliči") }
        }
    )
}
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    // Začasni podatki za testiranje tabele
    val database = remember {
        mutableStateListOf(
            Property(1, "Slovenska cesta 1", "Ljubljana", "Stanovanje", 50.0, 250000.0, 2020),
            Property(2, "Glavni trg 5", "Maribor", "Hiša", 120.0, 180000.0, 1995)
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // SIDEBAR
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xFF2C3E50))
                .padding(vertical = 20.dp)
        ) {
            Text(
                "Property DB",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Screen.values().forEach { screen ->
                NavigationItem(
                    screen = screen,
                    isSelected = currentScreen == screen,
                    onClick = { currentScreen = screen }
                )
            }
        }

        // CONTENT AREA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(currentScreen.title, style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)

                    if (currentScreen == Screen.Management) {
                        ActionButton("Dodaj Novo", onClick = { /* TODO: Odpri dialog */ }, icon = Icons.Default.Add)
                    }
                }

                when (currentScreen) {
                    Screen.Dashboard, Screen.Management -> {
                        DataTable(
                            properties = database,
                            onDelete = { id -> database.removeAll { it.id == id } },
                            onEdit = { /* TODO */ }
                        )
                    }
                    Screen.WebSources -> {
                        WebSourcesScreen(
                            onSendToDatabase = { selected ->
                                val nextId = (database.maxOfOrNull { it.id ?: 0 } ?: 0) + 1
                                selected.forEachIndexed { i, p ->
                                    database.add(p.copy(id = nextId + i))
                                }
                            }
                        )
                    }
                    else -> {
                        Text("Content for ${currentScreen.title} will be added in the next step.")
                    }
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

@Composable
fun WebSourcesScreen(
    onSendToDatabase: (List<Property>) -> Unit
) {
    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var filterCity by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val scope = rememberCoroutineScope()

    val filtered = properties.filter { p ->
        (filterCity.isBlank() || p.city.contains(filterCity, ignoreCase = true)) &&
                (filterType.isBlank() || p.type.contains(filterType, ignoreCase = true))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(
                text = if (isLoading) "Nalaganje..." else "Pridobi podatke",
                onClick = {
                    isLoading = true
                    scope.launch(Dispatchers.IO) {
                        val data = WebScraper.scrapeAll()
                        withContext(Dispatchers.Main) {
                            properties = data.mapIndexed { i, p -> p.copy(id = i) }
                            selectedIds = emptySet()
                            isLoading = false
                        }
                    }
                },
                icon = Icons.Default.CloudDownload
            )
            ActionButton(
                text = "Pošlji v bazo (${selectedIds.size})",
                onClick = {
                    val toSend = filtered.filter { it.id in selectedIds }
                    onSendToDatabase(toSend)
                    selectedIds = emptySet()
                },
                color = Color(0xFF27AE60),
                icon = Icons.Default.Save
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StyledTextField(filterCity, { filterCity = it }, "Filtriraj po mestu", Modifier.weight(1f))
            StyledTextField(filterType, { filterType = it }, "Filtriraj po tipu", Modifier.weight(1f))
        }

        Card(elevation = 4.dp, modifier = Modifier.fillMaxSize()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✓", modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold)
                    Text("Naslov", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text("Mesto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Cena", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Vir", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                Divider()
                LazyColumn {
                    items(filtered) { property ->
                        val checked = property.id in selectedIds
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    selectedIds = if (checked)
                                        selectedIds - (property.id ?: -1)
                                    else
                                        selectedIds + (property.id ?: -1)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                modifier = Modifier.weight(0.3f)
                            )
                            Text(property.address, modifier = Modifier.weight(2f))
                            Text(property.city, modifier = Modifier.weight(1f))
                            Text("${property.price} €", modifier = Modifier.weight(1f))
                            Text(property.description ?: "", modifier = Modifier.weight(1f))
                        }
                        Divider()
                    }
                }
            }
        }
    }
}