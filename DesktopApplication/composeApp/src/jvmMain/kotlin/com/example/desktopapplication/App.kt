package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import com.example.desktopapplication.models.PropertyMapper
import com.example.desktopapplication.models.User
import com.example.desktopapplication.models.UserMapper
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
fun PropertiesManagement(
    items: List<Property>,
    isLoading: Boolean,
    onEdit: (Property) -> Unit,
    onDelete: (Property) -> Unit,
    onBulkDelete: (List<Property>) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("") }
    var selectedApiIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var page by remember { mutableStateOf(0) }
    var detailsItem by remember { mutableStateOf<Property?>(null) }
    var pendingDelete by remember { mutableStateOf<Property?>(null) }
    var pendingBulkDelete by remember { mutableStateOf(false) }
    var sortKey by remember { mutableStateOf<String?>(null) }
    var sortDir by remember { mutableStateOf(SortDirection.ASC) }

    val filtered = remember(items, filter, typeFilter, sortKey, sortDir) {
        val q = filter.trim()
        val result = items.filter { p ->
            val matchesText = q.isBlank() || listOf(
                p.region, p.city, p.neighborhood, p.offerType, p.propertyType,
                p.description.orEmpty(), p.price.toInt().toString()
            ).any { it.contains(q, ignoreCase = true) }
            val matchesType = typeFilter.isBlank() || p.propertyType == typeFilter
            matchesText && matchesType
        }
        val sorted = when (sortKey) {
            "region" -> result.sortedBy { it.region.lowercase() }
            "city" -> result.sortedBy { (if (it.neighborhood.isNotBlank()) it.neighborhood else it.city).lowercase() }
            "offerType" -> result.sortedBy { it.offerType.lowercase() }
            "propertyType" -> result.sortedBy { it.propertyType.lowercase() }
            "size" -> result.sortedBy { it.size }
            "price" -> result.sortedBy { it.price }
            else -> result
        }
        if (sortKey != null && sortDir == SortDirection.DESC) sorted.reversed() else sorted
    }
    val totalPages = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val paged = filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
    val selectedItems = items.filter { (it.apiId ?: "") in selectedApiIds }
    val sortClick: (String) -> Unit = { k ->
        val (newKey, newDir) = nextSort(sortKey, sortDir, k); sortKey = newKey; sortDir = newDir
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StyledTextField(filter, { filter = it; page = 0 }, "Išči (regija, mesto, opis, cena ...)", Modifier.weight(2f))
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    label = "Tip (vsi)",
                    value = typeFilter.ifBlank { "vsi" },
                    options = listOf("vsi") + PROPERTY_TYPES
                ) { typeFilter = if (it == "vsi") "" else it; page = 0 }
            }
            if (selectedApiIds.isNotEmpty()) {
                ActionButton(
                    text = "Izbriši izbrane (${selectedApiIds.size})",
                    onClick = { pendingBulkDelete = true },
                    color = Color(0xFFC0392B),
                    icon = Icons.Default.Delete
                )
            }
        }

        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pageApiIds = paged.mapNotNull { it.apiId }.toSet()
                    val allSelectedOnPage = pageApiIds.isNotEmpty() && pageApiIds.all { it in selectedApiIds }
                    Checkbox(
                        checked = allSelectedOnPage,
                        onCheckedChange = { checked ->
                            selectedApiIds = if (checked) selectedApiIds + pageApiIds else selectedApiIds - pageApiIds
                        },
                        modifier = Modifier.weight(0.3f)
                    )
                    SortableHeader("Regija", "region", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    SortableHeader("Mesto / Naselje", "city", sortKey, sortDir, sortClick, Modifier.weight(2f))
                    SortableHeader("Ponudba", "offerType", sortKey, sortDir, sortClick, Modifier.weight(0.7f))
                    SortableHeader("Tip", "propertyType", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    SortableHeader("m²", "size", sortKey, sortDir, sortClick, Modifier.weight(0.5f))
                    SortableHeader("Cena", "price", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    Text("Akcije", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold)
                }
                Divider()

                when {
                    isLoading && items.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    filtered.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(if (items.isEmpty()) "Ni zapisov." else "Filter ne najde zadetkov.", color = Color(0xFF6B7280)) }
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 600.dp)) {
                        items(paged) { p ->
                            val apiId = p.apiId
                            val selected = apiId != null && apiId in selectedApiIds
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { checked ->
                                        if (apiId != null) {
                                            selectedApiIds = if (checked) selectedApiIds + apiId else selectedApiIds - apiId
                                        }
                                    },
                                    modifier = Modifier.weight(0.3f),
                                    enabled = apiId != null
                                )
                                Text(p.region, modifier = Modifier.weight(1f))
                                Text(
                                    if (p.neighborhood.isNotBlank()) "${p.neighborhood}, ${p.city}" else p.city,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(p.offerType, modifier = Modifier.weight(0.7f))
                                Text(p.propertyType, modifier = Modifier.weight(1f))
                                Text("${p.size.toInt()}", modifier = Modifier.weight(0.5f))
                                Text("${p.price.toInt()} €", modifier = Modifier.weight(1f))
                                Row(modifier = Modifier.weight(1.1f)) {
                                    IconButton(onClick = { detailsItem = p }) {
                                        Icon(Icons.Default.Visibility, "Podrobnosti", tint = Color(0xFF2980B9))
                                    }
                                    IconButton(onClick = { onEdit(p) }) {
                                        Icon(Icons.Default.Edit, "Uredi", tint = Color(0xFFF39C12))
                                    }
                                    IconButton(onClick = { pendingDelete = p }) {
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

        if (filtered.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filtered.size} zapisov · stran ${currentPage + 1} / $totalPages", color = Color(0xFF6B7280))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { page = (currentPage - 1).coerceAtLeast(0) }, enabled = currentPage > 0) {
                        Text("‹ Prejšnja")
                    }
                    Button(onClick = { page = (currentPage + 1).coerceAtMost(totalPages - 1) }, enabled = currentPage < totalPages - 1) {
                        Text("Naslednja ›")
                    }
                }
            }
        }
    }

    detailsItem?.let { p ->
        DetailsDialog(
            title = "Podrobnosti zapisa",
            fields = listOf(
                "Regija" to p.region,
                "Mesto / občina" to p.city,
                "Naselje" to p.neighborhood,
                "Tip ponudbe" to p.offerType,
                "Vrsta nepremičnine" to p.propertyType,
                "Velikost (m²)" to p.size.toString(),
                "Cena (€)" to p.price.toString(),
                "Opis" to (p.description ?: ""),
                "Povezava do oglasa" to (p.propertyLink ?: ""),
                "URL slike" to (p.imageUrl ?: ""),
                "Backend ID" to (p.apiId ?: "")
            ),
            onDismiss = { detailsItem = null }
        )
    }

    pendingDelete?.let { p ->
        ConfirmDeleteDialog(
            message = "Ali res želiš izbrisati zapis za ${p.city}${if (p.neighborhood.isNotBlank()) " (${p.neighborhood})" else ""}?",
            onConfirm = { onDelete(p); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }

    if (pendingBulkDelete) {
        ConfirmDeleteDialog(
            message = "Ali res želiš izbrisati ${selectedApiIds.size} izbranih zapisov?",
            onConfirm = {
                onBulkDelete(selectedItems)
                selectedApiIds = emptySet()
                pendingBulkDelete = false
            },
            onDismiss = { pendingBulkDelete = false }
        )
    }
}

val PROPERTY_TYPES = listOf("Stanovanje", "Hiša", "Vikend", "Poslovni prostor", "Garaža", "Parcela", "Počitniški objekt", "Soba")
val OFFER_TYPES = listOf("Prodaja", "Oddaja")
const val PAGE_SIZE = 10

enum class SortDirection { ASC, DESC }

@Composable
fun SortableHeader(
    text: String,
    sortKey: String,
    currentSortKey: String?,
    direction: SortDirection,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = currentSortKey == sortKey
    Row(
        modifier = modifier.clickable { onClick(sortKey) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontWeight = FontWeight.Bold)
        if (active) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (direction == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF2980B9)
            )
        }
    }
}

fun nextSort(current: String?, direction: SortDirection, clicked: String): Pair<String, SortDirection> {
    return if (current == clicked) {
        clicked to if (direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
    } else {
        clicked to SortDirection.ASC
    }
}

@Composable
fun ConfirmDeleteDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Potrditev brisanja") },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC0392B))
            ) { Text("Izbriši", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Prekliči") }
        }
    )
}

@Composable
fun DetailsDialog(
    title: String,
    fields: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.forEach { (label, value) ->
                    Column {
                        Text(label, fontSize = 12.sp, color = Color(0xFF6B7280))
                        Text(value.ifBlank { "—" }, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zapri") }
        }
    )
}

@Composable
fun DropdownSelector(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onSelected(option)
                    expanded = false
                }) { Text(option) }
            }
        }
    }
}

@Composable
fun PropertyEditDialog(
    property: Property? = null,
    onDismiss: () -> Unit,
    onConfirm: (Property, Double?, Double?) -> Unit
) {
    var region by remember { mutableStateOf(property?.region ?: "") }
    var city by remember { mutableStateOf(property?.city ?: "") }
    var neighborhood by remember { mutableStateOf(property?.neighborhood ?: "") }
    var offerType by remember { mutableStateOf(property?.offerType?.takeIf { it in OFFER_TYPES } ?: "Prodaja") }
    var propertyType by remember {
        mutableStateOf(property?.propertyType?.takeIf { it in PROPERTY_TYPES } ?: "Stanovanje")
    }
    var size by remember { mutableStateOf(property?.size?.toString() ?: "0") }
    var price by remember { mutableStateOf(property?.price?.toString() ?: "") }
    var description by remember { mutableStateOf(property?.description ?: "") }
    var propertyLink by remember { mutableStateOf(property?.propertyLink ?: "") }
    var imageUrl by remember { mutableStateOf(property?.imageUrl ?: "") }
    var lng by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun validate(): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (region.isBlank()) e["region"] = "Regija je obvezna"
        if (city.isBlank()) e["city"] = "Mesto je obvezno"
        val sizeNum = size.replace(",", ".").toDoubleOrNull()
        if (sizeNum == null || sizeNum < 0) e["size"] = "Velikost mora biti veljavno število ≥ 0"
        val priceNum = price.replace(",", ".").toDoubleOrNull()
        if (priceNum == null || priceNum < 0) e["price"] = "Cena mora biti veljavno število ≥ 0"
        if (lng.isNotBlank() && lng.replace(",", ".").toDoubleOrNull() == null)
            e["lng"] = "Neveljavna številka"
        if (lat.isNotBlank() && lat.replace(",", ".").toDoubleOrNull() == null)
            e["lat"] = "Neveljavna številka"
        return e
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (property == null) "Dodaj nov zapis" else "Uredi zapis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ValidatedField(region, { region = it }, "Regija", errors["region"])
                ValidatedField(city, { city = it }, "Mesto / občina", errors["city"])
                StyledTextField(neighborhood, { neighborhood = it }, "Naselje (opcijsko)")
                DropdownSelector("Tip ponudbe", offerType, OFFER_TYPES) { offerType = it }
                DropdownSelector("Vrsta nepremičnine", propertyType, PROPERTY_TYPES) { propertyType = it }
                ValidatedField(size, { size = it }, "Velikost (m²)", errors["size"])
                ValidatedField(price, { price = it }, "Cena (€)", errors["price"])
                StyledTextField(description, { description = it }, "Opis")
                StyledTextField(propertyLink, { propertyLink = it }, "Povezava do oglasa (URL)")
                StyledTextField(imageUrl, { imageUrl = it }, "URL slike")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValidatedField(lng, { lng = it }, "Geo. dolžina (lng) — opcijsko", errors["lng"], Modifier.weight(1f))
                    ValidatedField(lat, { lat = it }, "Geo. širina (lat) — opcijsko", errors["lat"], Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val e = validate()
                if (e.isNotEmpty()) { errors = e; return@Button }
                onConfirm(
                    Property(
                        id = property?.id,
                        apiId = property?.apiId,
                        region = region.trim(),
                        city = city.trim(),
                        neighborhood = neighborhood.trim(),
                        offerType = offerType,
                        propertyType = propertyType,
                        size = size.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        price = price.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        description = description.ifBlank { null },
                        propertyLink = propertyLink.ifBlank { null },
                        imageUrl = imageUrl.ifBlank { null }
                    ),
                    lng.replace(",", ".").toDoubleOrNull(),
                    lat.replace(",", ".").toDoubleOrNull()
                )
            }) { Text("Shrani") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Prekliči") }
        }
    )
}

@Composable
fun ValidatedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            isError = error != null
        )
        if (error != null) {
            Text(error, color = Color(0xFFC0392B), fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
        }
    }
}

@Composable
fun UserEditDialog(
    user: User? = null,
    onDismiss: () -> Unit,
    onConfirm: (User, String?) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var isAdmin by remember { mutableStateOf(user?.isAdmin ?: false) }
    var password by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun validate(): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (name.isBlank()) e["name"] = "Ime je obvezno"
        val emailOk = email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
        if (email.isBlank() || !emailOk) e["email"] = "Neveljaven e-poštni naslov"
        if (user == null && password.length < 4) e["password"] = "Geslo mora imeti vsaj 4 znake"
        return e
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Dodaj uporabnika" else "Uredi uporabnika") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ValidatedField(name, { name = it }, "Ime", errors["name"])
                ValidatedField(email, { email = it }, "E-pošta", errors["email"])
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAdmin, onCheckedChange = { isAdmin = it })
                    Text("Administrator")
                }
                ValidatedField(
                    password,
                    { password = it },
                    if (user == null) "Geslo" else "Novo geslo (prazno = brez spremembe)",
                    errors["password"]
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val e = validate()
                if (e.isNotEmpty()) { errors = e; return@Button }
                onConfirm(
                    User(
                        id = user?.id,
                        apiId = user?.apiId,
                        name = name.trim(),
                        email = email.trim(),
                        isAdmin = isAdmin
                    ),
                    password.ifBlank { null }
                )
            }) { Text("Shrani") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Prekliči") }
        }
    )
}

@Composable
fun UsersManagement(
    items: List<User>,
    isLoading: Boolean,
    onEdit: (User) -> Unit,
    onDelete: (User) -> Unit,
    onBulkDelete: (List<User>) -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var adminFilter by remember { mutableStateOf("") }
    var selectedApiIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var page by remember { mutableStateOf(0) }
    var detailsItem by remember { mutableStateOf<User?>(null) }
    var pendingDelete by remember { mutableStateOf<User?>(null) }
    var pendingBulkDelete by remember { mutableStateOf(false) }
    var sortKey by remember { mutableStateOf<String?>(null) }
    var sortDir by remember { mutableStateOf(SortDirection.ASC) }

    val filtered = remember(items, filter, adminFilter, sortKey, sortDir) {
        val q = filter.trim()
        val result = items.filter { u ->
            val matchesText = q.isBlank() || listOf(u.name, u.email).any { it.contains(q, ignoreCase = true) }
            val matchesAdmin = when (adminFilter) {
                "DA" -> u.isAdmin
                "NE" -> !u.isAdmin
                else -> true
            }
            matchesText && matchesAdmin
        }
        val sorted = when (sortKey) {
            "name" -> result.sortedBy { it.name.lowercase() }
            "email" -> result.sortedBy { it.email.lowercase() }
            "admin" -> result.sortedBy { it.isAdmin }
            else -> result
        }
        if (sortKey != null && sortDir == SortDirection.DESC) sorted.reversed() else sorted
    }
    val sortClick: (String) -> Unit = { k ->
        val (newKey, newDir) = nextSort(sortKey, sortDir, k); sortKey = newKey; sortDir = newDir
    }
    val totalPages = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val paged = filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
    val selectedItems = items.filter { (it.apiId ?: "") in selectedApiIds }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StyledTextField(filter, { filter = it; page = 0 }, "Išči (ime, e-pošta)", Modifier.weight(2f))
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector(
                    label = "Admin",
                    value = adminFilter.ifBlank { "vsi" },
                    options = listOf("vsi", "DA", "NE")
                ) { adminFilter = if (it == "vsi") "" else it; page = 0 }
            }
            if (selectedApiIds.isNotEmpty()) {
                ActionButton(
                    text = "Izbriši izbrane (${selectedApiIds.size})",
                    onClick = { pendingBulkDelete = true },
                    color = Color(0xFFC0392B),
                    icon = Icons.Default.Delete
                )
            }
        }

        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pageApiIds = paged.mapNotNull { it.apiId }.toSet()
                    val allSelectedOnPage = pageApiIds.isNotEmpty() && pageApiIds.all { it in selectedApiIds }
                    Checkbox(
                        checked = allSelectedOnPage,
                        onCheckedChange = { checked ->
                            selectedApiIds = if (checked) selectedApiIds + pageApiIds else selectedApiIds - pageApiIds
                        },
                        modifier = Modifier.weight(0.3f)
                    )
                    SortableHeader("Ime", "name", sortKey, sortDir, sortClick, Modifier.weight(2f))
                    SortableHeader("E-pošta", "email", sortKey, sortDir, sortClick, Modifier.weight(2f))
                    SortableHeader("Admin", "admin", sortKey, sortDir, sortClick, Modifier.weight(0.7f))
                    Text("Akcije", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold)
                }
                Divider()

                when {
                    isLoading && items.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    filtered.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(if (items.isEmpty()) "Ni zapisov." else "Filter ne najde zadetkov.", color = Color(0xFF6B7280)) }
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 600.dp)) {
                        items(paged) { u ->
                            val apiId = u.apiId
                            val selected = apiId != null && apiId in selectedApiIds
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { checked ->
                                        if (apiId != null) {
                                            selectedApiIds = if (checked) selectedApiIds + apiId else selectedApiIds - apiId
                                        }
                                    },
                                    modifier = Modifier.weight(0.3f),
                                    enabled = apiId != null
                                )
                                Text(u.name, modifier = Modifier.weight(2f))
                                Text(u.email, modifier = Modifier.weight(2f))
                                Text(if (u.isAdmin) "DA" else "NE", modifier = Modifier.weight(0.7f))
                                Row(modifier = Modifier.weight(1.1f)) {
                                    IconButton(onClick = { detailsItem = u }) {
                                        Icon(Icons.Default.Visibility, "Podrobnosti", tint = Color(0xFF2980B9))
                                    }
                                    IconButton(onClick = { onEdit(u) }) {
                                        Icon(Icons.Default.Edit, "Uredi", tint = Color(0xFFF39C12))
                                    }
                                    IconButton(onClick = { pendingDelete = u }) {
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

        if (filtered.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filtered.size} zapisov · stran ${currentPage + 1} / $totalPages", color = Color(0xFF6B7280))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { page = (currentPage - 1).coerceAtLeast(0) }, enabled = currentPage > 0) {
                        Text("‹ Prejšnja")
                    }
                    Button(onClick = { page = (currentPage + 1).coerceAtMost(totalPages - 1) }, enabled = currentPage < totalPages - 1) {
                        Text("Naslednja ›")
                    }
                }
            }
        }
    }

    detailsItem?.let { u ->
        DetailsDialog(
            title = "Podrobnosti uporabnika",
            fields = listOf(
                "Ime" to u.name,
                "E-pošta" to u.email,
                "Administrator" to if (u.isAdmin) "DA" else "NE",
                "Backend ID" to (u.apiId ?: "")
            ),
            onDismiss = { detailsItem = null }
        )
    }

    pendingDelete?.let { u ->
        ConfirmDeleteDialog(
            message = "Ali res želiš izbrisati uporabnika '${u.name}'?",
            onConfirm = { onDelete(u); pendingDelete = null },
            onDismiss = { pendingDelete = null }
        )
    }

    if (pendingBulkDelete) {
        ConfirmDeleteDialog(
            message = "Ali res želiš izbrisati ${selectedApiIds.size} izbranih uporabnikov?",
            onConfirm = {
                onBulkDelete(selectedItems)
                selectedApiIds = emptySet()
                pendingBulkDelete = false
            },
            onDismiss = { pendingBulkDelete = false }
        )
    }
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

private fun CoroutineScope.refreshUsers(
    onStart: () -> Unit,
    onResult: (List<User>, String?) -> Unit
) {
    onStart()
    launch(Dispatchers.IO) {
        val (data, error) = try {
            ApiClient.users.list().mapIndexed { i, dto ->
                UserMapper.fromResponse(dto, i + 1)
            } to null
        } catch (e: Exception) {
            emptyList<User>() to (e.message ?: "Napaka pri nalaganju")
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
                statusMessage = err?.let { "Napaka: $it" }
            }
        )
        scope.refreshUsers(
            onStart = {},
            onResult = { data, _ ->
                usersDb.clear()
                usersDb.addAll(data)
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

@Composable
fun WebSourcesScreen(onSendToDatabase: (List<Property>) -> Unit) {
    var properties by remember { mutableStateOf<List<Property>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("") }
    var filterOffer by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var minSize by remember { mutableStateOf("") }
    var maxSize by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var page by remember { mutableStateOf(0) }
    var sortKey by remember { mutableStateOf<String?>(null) }
    var sortDir by remember { mutableStateOf(SortDirection.ASC) }

    val scope = rememberCoroutineScope()

    val filtered = remember(properties, filterText, filterType, filterOffer, minPrice, maxPrice, minSize, maxSize, sortKey, sortDir) {
        val q = filterText.trim()
        val minP = minPrice.toDoubleOrNull()
        val maxP = maxPrice.toDoubleOrNull()
        val minS = minSize.toDoubleOrNull()
        val maxS = maxSize.toDoubleOrNull()
        val result = properties.filter { p ->
            val textOk = q.isBlank() || listOf(p.region, p.city, p.neighborhood, p.description.orEmpty())
                .any { it.contains(q, ignoreCase = true) }
            val typeOk = filterType.isBlank() || p.propertyType == filterType
            val offerOk = filterOffer.isBlank() || p.offerType == filterOffer
            val priceOk = (minP == null || p.price >= minP) && (maxP == null || p.price <= maxP)
            val sizeOk = (minS == null || p.size >= minS) && (maxS == null || p.size <= maxS)
            textOk && typeOk && offerOk && priceOk && sizeOk
        }
        val sorted = when (sortKey) {
            "city" -> result.sortedBy { (if (it.neighborhood.isNotBlank()) it.neighborhood else it.city).lowercase() }
            "propertyType" -> result.sortedBy { it.propertyType.lowercase() }
            "offerType" -> result.sortedBy { it.offerType.lowercase() }
            "size" -> result.sortedBy { it.size }
            "price" -> result.sortedBy { it.price }
            else -> result
        }
        if (sortKey != null && sortDir == SortDirection.DESC) sorted.reversed() else sorted
    }
    val sortClick: (String) -> Unit = { k ->
        val (newKey, newDir) = nextSort(sortKey, sortDir, k); sortKey = newKey; sortDir = newDir
    }
    val totalPages = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val paged = filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            page = 0
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyledTextField(filterText, { filterText = it; page = 0 }, "Išči (regija, mesto, opis)", Modifier.weight(2f))
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector("Tip (vsi)", filterType.ifBlank { "vsi" }, listOf("vsi") + PROPERTY_TYPES) {
                    filterType = if (it == "vsi") "" else it; page = 0
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                DropdownSelector("Ponudba (vse)", filterOffer.ifBlank { "vse" }, listOf("vse") + OFFER_TYPES) {
                    filterOffer = if (it == "vse") "" else it; page = 0
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyledTextField(minPrice, { minPrice = it; page = 0 }, "Cena od (€)", Modifier.weight(1f))
            StyledTextField(maxPrice, { maxPrice = it; page = 0 }, "Cena do (€)", Modifier.weight(1f))
            StyledTextField(minSize, { minSize = it; page = 0 }, "m² od", Modifier.weight(1f))
            StyledTextField(maxSize, { maxSize = it; page = 0 }, "m² do", Modifier.weight(1f))
        }

        Card(elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pageIds = paged.mapNotNull { it.id }.toSet()
                    val allSelectedOnPage = pageIds.isNotEmpty() && pageIds.all { it in selectedIds }
                    Checkbox(
                        checked = allSelectedOnPage,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + pageIds else selectedIds - pageIds
                        },
                        modifier = Modifier.weight(0.3f)
                    )
                    SortableHeader("Lokacija", "city", sortKey, sortDir, sortClick, Modifier.weight(2f))
                    SortableHeader("Tip", "propertyType", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    SortableHeader("Ponudba", "offerType", sortKey, sortDir, sortClick, Modifier.weight(0.7f))
                    SortableHeader("m²", "size", sortKey, sortDir, sortClick, Modifier.weight(0.5f))
                    SortableHeader("Cena", "price", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    Text("Opis", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                }
                Divider()
                when {
                    isLoading && properties.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    filtered.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(if (properties.isEmpty()) "Klikni 'Pridobi podatke' za začetek." else "Filter ne najde zadetkov.", color = Color(0xFF6B7280)) }
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 600.dp)) {
                        items(paged) { property ->
                            val checked = property.id in selectedIds
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { c ->
                                        val id = property.id ?: return@Checkbox
                                        selectedIds = if (c) selectedIds + id else selectedIds - id
                                    },
                                    modifier = Modifier.weight(0.3f)
                                )
                                Text(
                                    if (property.neighborhood.isNotBlank()) "${property.neighborhood}, ${property.city}" else property.city,
                                    modifier = Modifier.weight(2f),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(property.propertyType, modifier = Modifier.weight(1f))
                                Text(property.offerType, modifier = Modifier.weight(0.7f))
                                Text("${property.size.toInt()}", modifier = Modifier.weight(0.5f))
                                Text("${property.price.toInt()} €", modifier = Modifier.weight(1f))
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

        if (filtered.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${filtered.size} zapisov · stran ${currentPage + 1} / $totalPages", color = Color(0xFF6B7280))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { page = (currentPage - 1).coerceAtLeast(0) }, enabled = currentPage > 0) {
                        Text("‹ Prejšnja")
                    }
                    Button(onClick = { page = (currentPage + 1).coerceAtMost(totalPages - 1) }, enabled = currentPage < totalPages - 1) {
                        Text("Naslednja ›")
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
    var page by remember { mutableStateOf(0) }
    var sortKey by remember { mutableStateOf<String?>(null) }
    var sortDir by remember { mutableStateOf(SortDirection.ASC) }

    val sortedGenerated = remember(generated, sortKey, sortDir) {
        val sorted = when (sortKey) {
            "region" -> generated.sortedBy { it.region.lowercase() }
            "city" -> generated.sortedBy { (if (it.neighborhood.isNotBlank()) it.neighborhood else it.city).lowercase() }
            "propertyType" -> generated.sortedBy { it.propertyType.lowercase() }
            "offerType" -> generated.sortedBy { it.offerType.lowercase() }
            "size" -> generated.sortedBy { it.size }
            "price" -> generated.sortedBy { it.price }
            else -> generated
        }
        if (sortKey != null && sortDir == SortDirection.DESC) sorted.reversed() else sorted
    }
    val totalPages = ((sortedGenerated.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val paged = sortedGenerated.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
    val sortClick: (String) -> Unit = { k ->
        val (newKey, newDir) = nextSort(sortKey, sortDir, k); sortKey = newKey; sortDir = newDir
    }

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
                            page = 0
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
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFECF0F1)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pageIds = paged.mapNotNull { it.id }.toSet()
                        val allSelectedOnPage = pageIds.isNotEmpty() && pageIds.all { it in selectedIds }
                        Checkbox(
                            checked = allSelectedOnPage,
                            onCheckedChange = { c ->
                                selectedIds = if (c) selectedIds + pageIds else selectedIds - pageIds
                            },
                            modifier = Modifier.weight(0.3f)
                        )
                        SortableHeader("Regija", "region", sortKey, sortDir, sortClick, Modifier.weight(1f))
                        SortableHeader("Mesto / Naselje", "city", sortKey, sortDir, sortClick, Modifier.weight(2f))
                        SortableHeader("Tip", "propertyType", sortKey, sortDir, sortClick, Modifier.weight(1f))
                        SortableHeader("Ponudba", "offerType", sortKey, sortDir, sortClick, Modifier.weight(0.7f))
                        SortableHeader("m²", "size", sortKey, sortDir, sortClick, Modifier.weight(0.5f))
                        SortableHeader("Cena", "price", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    }
                    Divider()
                    LazyColumn(modifier = Modifier.heightIn(max = 600.dp)) {
                        items(paged) { p ->
                            val checked = p.id in selectedIds
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { c ->
                                        val id = p.id ?: return@Checkbox
                                        selectedIds = if (c) selectedIds + id else selectedIds - id
                                    },
                                    modifier = Modifier.weight(0.3f)
                                )
                                Text(p.region, modifier = Modifier.weight(1f))
                                Text(
                                    if (p.neighborhood.isNotBlank()) "${p.neighborhood}, ${p.city}" else p.city,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(p.propertyType, modifier = Modifier.weight(1f))
                                Text(p.offerType, modifier = Modifier.weight(0.7f))
                                Text("${p.size.toInt()}", modifier = Modifier.weight(0.5f))
                                Text("${p.price.toInt()} €", modifier = Modifier.weight(1f))
                            }
                            Divider()
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${sortedGenerated.size} zapisov · stran ${currentPage + 1} / $totalPages", color = Color(0xFF6B7280))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { page = (currentPage - 1).coerceAtLeast(0) }, enabled = currentPage > 0) {
                        Text("‹ Prejšnja")
                    }
                    Button(onClick = { page = (currentPage + 1).coerceAtMost(totalPages - 1) }, enabled = currentPage < totalPages - 1) {
                        Text("Naslednja ›")
                    }
                }
            }
        }
    }
}
