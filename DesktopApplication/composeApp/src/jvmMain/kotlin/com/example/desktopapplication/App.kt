package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import com.example.desktopapplication.models.PropertyMapper
import com.example.desktopapplication.models.User
import com.example.desktopapplication.models.UserMapper
import com.example.desktopapplication.network.ApiClient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen(val title: String, val icon: ImageVector) {
    ManageData("Upravljaj podatke", Icons.Default.Storage),
    WebSources("Pridobi s spleta", Icons.Default.CloudDownload),
    Generator("Generator podatkov", Icons.Default.Build)
}

data class TableInfo(val key: String, val displayName: String, val icon: ImageVector)

val AVAILABLE_TABLES = listOf(
    TableInfo("properties", "Nepremičnine", Icons.Default.Home),
    TableInfo("users", "Uporabniki", Icons.Default.Person)
)

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.ManageData) }
    var selectedTable by remember { mutableStateOf<TableInfo?>(null) }
    val database = remember { mutableStateListOf<Property>() }
    val usersDb = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var editingProperty by remember { mutableStateOf<Property?>(null) }
    var editingUser by remember { mutableStateOf<User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun reload() {
        scope.refreshProperties(
            onStart = { isLoading = true; statusMessage = null },
            onResult = { data, err ->
                database.clear()
                database.addAll(data)
                isLoading = false
                if (err != null) statusMessage = "Napaka: $err"
            }
        )
        scope.refreshUsers(
            onStart = {},
            onResult = { data, err ->
                usersDb.clear()
                usersDb.addAll(data)
                if (err != null) statusMessage = "Napaka pri uporabnikih: $err"
            }
        )
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(4000)
            statusMessage = null
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
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
                    onClick = {
                        currentScreen = screen
                        if (screen != Screen.ManageData) selectedTable = null
                    }
                )
            }
        }

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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentScreen == Screen.ManageData && selectedTable != null) {
                            IconButton(onClick = { selectedTable = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Nazaj")
                            }
                        }
                        val titleText = when {
                            currentScreen == Screen.ManageData && selectedTable != null ->
                                selectedTable!!.displayName
                            else -> currentScreen.title
                        }
                        Text(titleText, style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = if (isLoading) "Nalagam..." else "Osveži",
                            onClick = { reload() },
                            icon = Icons.Default.Refresh,
                            enabled = !isLoading
                        )
                        if (currentScreen == Screen.ManageData && selectedTable != null) {
                            ActionButton(
                                "Dodaj Novo",
                                onClick = { showAddDialog = true },
                                icon = Icons.Default.Add
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Text(
                        statusMessage!!,
                        color = if (statusMessage!!.startsWith("Napaka")) Color(0xFFC0392B) else Color(0xFF27AE60)
                    )
                }

                when (currentScreen) {
                    Screen.ManageData -> {
                        if (selectedTable == null) {
                            TablesListScreen(
                                tables = AVAILABLE_TABLES,
                                rowCounts = mapOf(
                                    "properties" to database.size,
                                    "users" to usersDb.size
                                ),
                                onTableClick = { selectedTable = it }
                            )
                        } else when (selectedTable!!.key) {
                            "properties" -> PropertiesManagement(
                                items = database,
                                isLoading = isLoading,
                                onEdit = { editingProperty = it },
                                onDelete = { p ->
                                    val apiId = p.apiId
                                    if (apiId == null) {
                                        statusMessage = "Napaka: zapis nima backend ID-ja"
                                        return@PropertiesManagement
                                    }
                                    val idx = database.indexOfFirst { it.apiId == apiId }
                                    val removed = if (idx >= 0) database.removeAt(idx) else null
                                    scope.launch(Dispatchers.IO) {
                                        val err = try {
                                            ApiClient.properties.delete(apiId); null
                                        } catch (e: Exception) { e.message ?: "Napaka pri brisanju" }
                                        withContext(Dispatchers.Main) {
                                            if (err != null) {
                                                statusMessage = "Napaka: $err"
                                                if (removed != null && idx >= 0) database.add(idx.coerceAtMost(database.size), removed)
                                            } else {
                                                statusMessage = "Zapis izbrisan."
                                            }
                                        }
                                    }
                                },
                                onBulkDelete = { selected ->
                                    val apiIds = selected.mapNotNull { it.apiId }.toSet()
                                    val original = database.toList()
                                    database.removeAll { it.apiId in apiIds }
                                    scope.launch(Dispatchers.IO) {
                                        var failed = 0
                                        for (id in apiIds) {
                                            try { ApiClient.properties.delete(id) } catch (e: Exception) { failed++ }
                                        }
                                        withContext(Dispatchers.Main) {
                                            if (failed > 0) {
                                                statusMessage = "Brisanje: ${apiIds.size - failed} uspešno, $failed neuspešno"
                                                database.clear(); database.addAll(original)
                                                reload()
                                            } else {
                                                statusMessage = "Izbrisano ${apiIds.size} zapisov."
                                            }
                                        }
                                    }
                                }
                            )
                            "users" -> UsersManagement(
                                items = usersDb,
                                isLoading = isLoading,
                                onEdit = { editingUser = it },
                                onDelete = { u ->
                                    val apiId = u.apiId
                                    if (apiId == null) {
                                        statusMessage = "Napaka: uporabnik nima backend ID-ja"
                                        return@UsersManagement
                                    }
                                    val idx = usersDb.indexOfFirst { it.apiId == apiId }
                                    val removed = if (idx >= 0) usersDb.removeAt(idx) else null
                                    scope.launch(Dispatchers.IO) {
                                        val err = try {
                                            ApiClient.users.delete(apiId); null
                                        } catch (e: Exception) { e.message ?: "Napaka pri brisanju" }
                                        withContext(Dispatchers.Main) {
                                            if (err != null) {
                                                statusMessage = "Napaka: $err"
                                                if (removed != null && idx >= 0) usersDb.add(idx.coerceAtMost(usersDb.size), removed)
                                            } else {
                                                statusMessage = "Uporabnik izbrisan."
                                            }
                                        }
                                    }
                                },
                                onBulkDelete = { selected ->
                                    val apiIds = selected.mapNotNull { it.apiId }.toSet()
                                    val original = usersDb.toList()
                                    usersDb.removeAll { it.apiId in apiIds }
                                    scope.launch(Dispatchers.IO) {
                                        var failed = 0
                                        for (id in apiIds) {
                                            try { ApiClient.users.delete(id) } catch (e: Exception) { failed++ }
                                        }
                                        withContext(Dispatchers.Main) {
                                            if (failed > 0) {
                                                statusMessage = "Brisanje: ${apiIds.size - failed} uspešno, $failed neuspešno"
                                                usersDb.clear(); usersDb.addAll(original)
                                                reload()
                                            } else {
                                                statusMessage = "Izbrisanih ${apiIds.size} uporabnikov."
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Screen.WebSources -> {
                        WebSourcesScreen(
                            onSendToDatabase = { selected ->
                                statusMessage = "Pošiljam ${selected.size} zapisov..."
                                scope.ingestAll(selected) { savedCount, err ->
                                    statusMessage = if (err != null)
                                        "Shranjeno $savedCount, napaka: $err"
                                    else
                                        "Uspešno shranjenih $savedCount zapisov."
                                    reload()
                                }
                            }
                        )
                    }
                    Screen.Generator -> {
                        GeneratorScreen(
                            onSendToDatabase = { selected ->
                                statusMessage = "Pošiljam ${selected.size} zapisov..."
                                scope.ingestAll(selected) { savedCount, err ->
                                    statusMessage = if (err != null)
                                        "Shranjeno $savedCount, napaka: $err"
                                    else
                                        "Uspešno shranjenih $savedCount zapisov."
                                    reload()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog && selectedTable?.key == "properties") {
        PropertyEditDialog(
            property = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newProp, lng, lat ->
                showAddDialog = false
                scope.launch(Dispatchers.IO) {
                    val err = try {
                        ApiClient.properties.create(PropertyMapper.toIngest(newProp, lng, lat)); null
                    } catch (e: Exception) {
                        e.message ?: "Napaka pri ustvarjanju"
                    }
                    withContext(Dispatchers.Main) {
                        statusMessage = if (err != null) "Napaka: $err" else "Dodano."
                        reload()
                    }
                }
            }
        )
    }

    if (showAddDialog && selectedTable?.key == "users") {
        UserEditDialog(
            user = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newUser, password ->
                showAddDialog = false
                scope.launch(Dispatchers.IO) {
                    val err = try {
                        ApiClient.users.create(UserMapper.toIngest(newUser, password)); null
                    } catch (e: Exception) {
                        e.message ?: "Napaka pri ustvarjanju"
                    }
                    withContext(Dispatchers.Main) {
                        statusMessage = if (err != null) "Napaka: $err" else "Uporabnik dodan."
                        reload()
                    }
                }
            }
        )
    }

    editingProperty?.let { editing ->
        PropertyEditDialog(
            property = editing,
            onDismiss = { editingProperty = null },
            onConfirm = { updated, lng, lat ->
                editingProperty = null
                val apiId = updated.apiId
                if (apiId == null) {
                    statusMessage = "Napaka: zapis nima backend ID-ja"
                    return@PropertyEditDialog
                }
                scope.launch(Dispatchers.IO) {
                    val err = try {
                        ApiClient.properties.update(apiId, PropertyMapper.toIngest(updated, lng, lat)); null
                    } catch (e: Exception) {
                        e.message ?: "Napaka pri posodabljanju"
                    }
                    withContext(Dispatchers.Main) {
                        statusMessage = if (err != null) "Napaka: $err" else "Posodobljeno."
                        reload()
                    }
                }
            }
        )
    }

    editingUser?.let { editing ->
        UserEditDialog(
            user = editing,
            onDismiss = { editingUser = null },
            onConfirm = { updated, password ->
                editingUser = null
                val apiId = updated.apiId
                if (apiId == null) {
                    statusMessage = "Napaka: uporabnik nima backend ID-ja"
                    return@UserEditDialog
                }
                scope.launch(Dispatchers.IO) {
                    val err = try {
                        ApiClient.users.update(apiId, UserMapper.toIngest(updated, password)); null
                    } catch (e: Exception) {
                        e.message ?: "Napaka pri posodabljanju"
                    }
                    withContext(Dispatchers.Main) {
                        statusMessage = if (err != null) "Napaka: $err" else "Uporabnik posodobljen."
                        reload()
                    }
                }
            }
        )
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
fun TablesListScreen(
    tables: List<TableInfo>,
    rowCounts: Map<String, Int>,
    onTableClick: (TableInfo) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Izberi tabelo za urejanje:",
            style = MaterialTheme.typography.subtitle1,
            color = Color(0xFF6B7280)
        )
        tables.forEach { table ->
            Card(
                elevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTableClick(table) }
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = table.icon,
                        contentDescription = null,
                        tint = Color(0xFF2980B9),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            table.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${rowCounts[table.key] ?: 0} zapisov",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}
