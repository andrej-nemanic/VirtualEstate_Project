package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import com.example.desktopapplication.models.PropertyMapper
import com.example.desktopapplication.network.ApiClient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Pregled podatkov", Icons.Default.List),
    Management("Upravljanje", Icons.Default.Edit),
    WebSources("Pridobi s spleta", Icons.Default.CloudDownload),
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
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(45.dp),
        enabled = enabled
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
    onDelete: (Property) -> Unit,
    onEdit: (Property) -> Unit,
    readOnly: Boolean = false
) {
    Card(elevation = 4.dp, modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Naslov", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                Text("Mesto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Tip", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Cena", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                if (!readOnly) {
                    Text("Akcije", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold)
                }
            }
            Divider()
            LazyColumn {
                items(properties) { property ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(property.address, modifier = Modifier.weight(2f))
                        Text(property.city, modifier = Modifier.weight(1f))
                        Text(property.type, modifier = Modifier.weight(1f))
                        Text("${property.price} €", modifier = Modifier.weight(1f))
                        if (!readOnly) {
                            Row(modifier = Modifier.weight(0.8f)) {
                                IconButton(onClick = { onEdit(property) }) {
                                    Icon(Icons.Default.Edit, "Uredi", tint = Color(0xFFF39C12))
                                }
                                IconButton(onClick = { onDelete(property) }) {
                                    Icon(Icons.Default.Delete, "Izbriši", tint = Color(0xFFC0392B))
                                }
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

val PROPERTY_TYPES = listOf("Stanovanje", "Hiša", "Vikend", "Poslovni prostor", "Garaža", "Zemljišče")

@Composable
fun PropertyEditDialog(
    property: Property? = null,
    onDismiss: () -> Unit,
    onConfirm: (Property, Double?, Double?) -> Unit
) {
    var address by remember { mutableStateOf(property?.address ?: "") }
    var city by remember { mutableStateOf(property?.city ?: "") }
    var type by remember {
        mutableStateOf(property?.type?.takeIf { it in PROPERTY_TYPES } ?: "Stanovanje")
    }
    var size by remember { mutableStateOf(property?.size?.toString() ?: "0") }
    var price by remember { mutableStateOf(property?.price?.toString() ?: "") }
    var buildYear by remember { mutableStateOf(property?.buildYear?.toString() ?: "2024") }
    var description by remember { mutableStateOf(property?.description ?: "") }
    var lng by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (property == null) "Dodaj nov zapis" else "Uredi zapis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StyledTextField(address, { address = it }, "Naslov")
                StyledTextField(city, { city = it }, "Mesto")
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text("Tip") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            IconButton(onClick = { typeMenuExpanded = !typeMenuExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        PROPERTY_TYPES.forEach { option ->
                            DropdownMenuItem(onClick = {
                                type = option
                                typeMenuExpanded = false
                            }) {
                                Text(option)
                            }
                        }
                    }
                }
                StyledTextField(size, { size = it }, "Velikost (m²)")
                StyledTextField(price, { price = it }, "Cena (€)")
                StyledTextField(buildYear, { buildYear = it }, "Leto izgradnje")
                StyledTextField(description, { description = it }, "Opis")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyledTextField(lng, { lng = it }, "Geo. dolžina (lng) — opcijsko", Modifier.weight(1f))
                    StyledTextField(lat, { lat = it }, "Geo. širina (lat) — opcijsko", Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    Property(
                        id = property?.id,
                        apiId = property?.apiId,
                        address = address,
                        city = city,
                        type = type,
                        size = size.toDoubleOrNull() ?: 0.0,
                        price = price.toDoubleOrNull() ?: 0.0,
                        buildYear = buildYear.toIntOrNull() ?: 2024,
                        description = description.ifBlank { null }
                    ),
                    lng.toDoubleOrNull(),
                    lat.toDoubleOrNull()
                )
            }) { Text("Shrani") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Prekliči") }
        }
    )
}

private fun CoroutineScope.refreshProperties(
    onStart: () -> Unit,
    onResult: (List<Property>, String?) -> Unit
) {
    onStart()
    launch(Dispatchers.IO) {
        val (data, error) = try {
            ApiClient.properties.list().mapIndexed { i, dto ->
                PropertyMapper.fromResponse(dto, i + 1)
            } to null
        } catch (e: Exception) {
            emptyList<Property>() to (e.message ?: "Napaka pri nalaganju")
        }
        withContext(Dispatchers.Main) { onResult(data, error) }
    }
}

private fun CoroutineScope.ingestAll(
    properties: List<Property>,
    onDone: (Int, String?) -> Unit
) {
    launch(Dispatchers.IO) {
        var saved = 0
        var lastError: String? = null
        for (p in properties) {
            try {
                ApiClient.properties.create(PropertyMapper.toIngest(p))
                saved++
            } catch (e: Exception) {
                lastError = e.message ?: "Napaka pri pošiljanju"
            }
        }
        withContext(Dispatchers.Main) { onDone(saved, lastError) }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    val database = remember { mutableStateListOf<Property>() }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var editingProperty by remember { mutableStateOf<Property?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun reload() {
        scope.refreshProperties(
            onStart = { isLoading = true; statusMessage = null },
            onResult = { data, err ->
                database.clear()
                database.addAll(data)
                isLoading = false
                statusMessage = err?.let { "Napaka: $it" }
            }
        )
    }

    LaunchedEffect(Unit) { reload() }

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
                    onClick = { currentScreen = screen }
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
                    Text(currentScreen.title, style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = if (isLoading) "Nalagam..." else "Osveži",
                            onClick = { reload() },
                            icon = Icons.Default.Refresh,
                            enabled = !isLoading
                        )
                        if (currentScreen == Screen.Management) {
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
                    Screen.Dashboard -> {
                        DataTable(
                            properties = database,
                            onDelete = {},
                            onEdit = {},
                            readOnly = true
                        )
                    }
                    Screen.Management -> {
                        DataTable(
                            properties = database,
                            onDelete = { p ->
                                val apiId = p.apiId
                                if (apiId == null) {
                                    statusMessage = "Napaka: zapis nima backend ID-ja"
                                    return@DataTable
                                }
                                scope.launch(Dispatchers.IO) {
                                    val err = try {
                                        ApiClient.properties.delete(apiId); null
                                    } catch (e: Exception) {
                                        e.message ?: "Napaka pri brisanju"
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (err != null) statusMessage = "Napaka: $err"
                                        else {
                                            statusMessage = "Zapis izbrisan."
                                            reload()
                                        }
                                    }
                                }
                            },
                            onEdit = { editingProperty = it }
                        )
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

    if (showAddDialog) {
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
fun WebSourcesScreen(onSendToDatabase: (List<Property>) -> Unit) {
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
                    Text("Opis", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
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
                            Text(property.address, modifier = Modifier.weight(2f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(property.city, modifier = Modifier.weight(1f))
                            Text("${property.price} €", modifier = Modifier.weight(1f))
                            Text(
                                property.description ?: "",
                                modifier = Modifier.weight(1.5f),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratorScreen(onSendToDatabase: (List<Property>) -> Unit) {
    var count by remember { mutableStateOf("10") }
    var priceMin by remember { mutableStateOf("50000") }
    var priceMax by remember { mutableStateOf("500000") }
    var sizeMin by remember { mutableStateOf("30") }
    var sizeMax by remember { mutableStateOf("200") }
    var yearMin by remember { mutableStateOf("1950") }
    var yearMax by remember { mutableStateOf("2024") }

    var generated by remember { mutableStateOf<List<Property>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(elevation = 4.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Parametri generiranja", fontWeight = FontWeight.Bold)
                StyledTextField(count, { count = it }, "Število zapisov")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StyledTextField(priceMin, { priceMin = it }, "Cena min (€)", Modifier.weight(1f))
                    StyledTextField(priceMax, { priceMax = it }, "Cena max (€)", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StyledTextField(sizeMin, { sizeMin = it }, "Velikost min (m²)", Modifier.weight(1f))
                    StyledTextField(sizeMax, { sizeMax = it }, "Velikost max (m²)", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StyledTextField(yearMin, { yearMin = it }, "Leto min", Modifier.weight(1f))
                    StyledTextField(yearMax, { yearMax = it }, "Leto max", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(
                        text = "Generiraj",
                        onClick = {
                            generated = DataGenerator.generate(
                                count = count.toIntOrNull() ?: 10,
                                priceRange = (priceMin.toIntOrNull() ?: 50000)..(priceMax.toIntOrNull() ?: 500000),
                                sizeRange = (sizeMin.toIntOrNull() ?: 30)..(sizeMax.toIntOrNull() ?: 200),
                                yearRange = (yearMin.toIntOrNull() ?: 1950)..(yearMax.toIntOrNull() ?: 2024)
                            )
                            selectedIds = generated.mapNotNull { it.id }.toSet()
                        },
                        icon = Icons.Default.Casino
                    )
                    ActionButton(
                        text = "Pošlji v bazo (${selectedIds.size})",
                        onClick = {
                            onSendToDatabase(generated.filter { it.id in selectedIds })
                            generated = emptyList()
                            selectedIds = emptySet()
                        },
                        color = Color(0xFF27AE60),
                        icon = Icons.Default.Save
                    )
                }
            }
        }

        if (generated.isNotEmpty()) {
            Card(elevation = 4.dp, modifier = Modifier.fillMaxSize()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp)
                    ) {
                        Text("✓", modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold)
                        Text("Naslov", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                        Text("Mesto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("Tip", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("m²", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
                        Text("Cena", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("Leto", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    LazyColumn {
                        items(generated) { p ->
                            val checked = p.id in selectedIds
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (checked) selectedIds - (p.id ?: -1)
                                        else selectedIds + (p.id ?: -1)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.weight(0.3f))
                                Text(p.address, modifier = Modifier.weight(2f))
                                Text(p.city, modifier = Modifier.weight(1f))
                                Text(p.type, modifier = Modifier.weight(1f))
                                Text("${p.size.toInt()}", modifier = Modifier.weight(0.7f))
                                Text("${p.price.toInt()} €", modifier = Modifier.weight(1f))
                                Text("${p.buildYear}", modifier = Modifier.weight(0.7f))
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
