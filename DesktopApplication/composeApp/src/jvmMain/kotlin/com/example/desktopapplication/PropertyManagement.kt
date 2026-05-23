package com.example.desktopapplication

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.desktopapplication.models.Property

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

    val q = filter.trim()
    val t = typeFilter.trim()
    val result = items.filter { p ->
        val matchesText = q.isBlank() || listOf(
            p.region, p.city, p.neighborhood, p.offerType, p.propertyType,
            p.description.orEmpty(), p.price.toInt().toString(), p.source.orEmpty()
        ).any { it.contains(q, ignoreCase = true) }
        val matchesType = t.isBlank() || p.propertyType.contains(t, ignoreCase = true)
        matchesText && matchesType
    }
    val sorted = when (sortKey) {
        "region" -> result.sortedBy { it.region.lowercase() }
        "city" -> result.sortedBy { (if (it.neighborhood.isNotBlank()) it.neighborhood else it.city).lowercase() }
        "offerType" -> result.sortedBy { it.offerType.lowercase() }
        "propertyType" -> result.sortedBy { it.propertyType.lowercase() }
        "size" -> result.sortedBy { it.size }
        "price" -> result.sortedBy { it.price }
        "source" -> result.sortedBy { it.source.orEmpty().lowercase() }
        else -> result
    }
    val filtered = if (sortKey != null && sortDir == SortDirection.DESC) sorted.reversed() else sorted
    val totalPages = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val paged = filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
    val selectedItems = items.filter { (it.apiId ?: "") in selectedApiIds }
    val sortClick: (String) -> Unit = { k ->
        val (newKey, newDir) = nextSort(sortKey, sortDir, k); sortKey = newKey; sortDir = newDir
    }

    LaunchedEffect(filter, typeFilter) { selectedApiIds = emptySet() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StyledTextField(filter, { filter = it; page = 0 }, "Išči (regija, mesto, opis, cena ...)", Modifier.weight(2f))
            StyledTextField(typeFilter, { typeFilter = it; page = 0 }, "Tip", Modifier.weight(1f))
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
                    val allApiIds = filtered.mapNotNull { it.apiId }.toSet()
                    val allSelected = allApiIds.isNotEmpty() && allApiIds.all { it in selectedApiIds }
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            selectedApiIds = if (checked) selectedApiIds + allApiIds else selectedApiIds - allApiIds
                        },
                        modifier = Modifier.weight(0.3f)
                    )
                    SortableHeader("Regija", "region", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    SortableHeader("Mesto / Naselje", "city", sortKey, sortDir, sortClick, Modifier.weight(2f))
                    SortableHeader("Ponudba", "offerType", sortKey, sortDir, sortClick, Modifier.weight(0.7f))
                    SortableHeader("Tip", "propertyType", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    SortableHeader("Vir", "source", sortKey, sortDir, sortClick, Modifier.weight(0.8f))
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
                                Text(p.source ?: "—", modifier = Modifier.weight(0.8f), color = Color(0xFF6B7280))
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("${filtered.size} zapisov · stran ${currentPage + 1} / $totalPages", color = Color(0xFF6B7280))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton("‹ Prejšnja", { page = (currentPage - 1).coerceAtLeast(0) }, enabled = currentPage > 0)
                    ActionButton("Naslednja ›", { page = (currentPage + 1).coerceAtMost(totalPages - 1) }, enabled = currentPage < totalPages - 1)
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
                "Vir" to (p.source ?: ""),
                "Koordinate (lng, lat)" to if (p.lng != null && p.lat != null) "${p.lng}, ${p.lat}" else "",
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
    var propertyType by remember { mutableStateOf(property?.propertyType ?: "") }
    var size by remember { mutableStateOf(property?.size?.toString() ?: "0") }
    var price by remember { mutableStateOf(property?.price?.toString() ?: "") }
    var description by remember { mutableStateOf(property?.description ?: "") }
    var propertyLink by remember { mutableStateOf(property?.propertyLink ?: "") }
    var imageUrl by remember { mutableStateOf(property?.imageUrl ?: "") }
    var source by remember { mutableStateOf(property?.source ?: "ročno") }
    var lng by remember { mutableStateOf(property?.lng?.toString() ?: "") }
    var lat by remember { mutableStateOf(property?.lat?.toString() ?: "") }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun validate(): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (region.isBlank()) e["region"] = "Regija je obvezna"
        if (city.isBlank()) e["city"] = "Mesto je obvezno"
        if (propertyType.isBlank()) e["propertyType"] = "Vrsta je obvezna"
        val sizeNum = size.replace(",", ".").toDoubleOrNull()
        if (sizeNum == null || sizeNum < 0) e["size"] = "Velikost mora biti veljavno število ≥ 0"
        val priceNum = price.replace(",", ".").toDoubleOrNull()
        if (priceNum == null || priceNum < 0) e["price"] = "Cena mora biti veljavno število ≥ 0"
        val lngOk = lng.isBlank() || lng.replace(",", ".").toDoubleOrNull() != null
        val latOk = lat.isBlank() || lat.replace(",", ".").toDoubleOrNull() != null
        if (!lngOk) e["lng"] = "Neveljavna številka"
        if (!latOk) e["lat"] = "Neveljavna številka"
        val lngFilled = lng.isNotBlank()
        val latFilled = lat.isNotBlank()
        if (lngFilled != latFilled) {
            e["lng"] = "Vnesi oboje (lng + lat) ali nobeno"
            e["lat"] = "Vnesi oboje (lng + lat) ali nobeno"
        }
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
                ValidatedField(propertyType, { propertyType = it }, "Vrsta nepremičnine (npr. Stanovanje, Hiša, Parcela)", errors["propertyType"])
                ValidatedField(size, { size = it }, "Velikost (m²)", errors["size"])
                ValidatedField(price, { price = it }, "Cena (€)", errors["price"])
                StyledTextField(description, { description = it }, "Opis")
                StyledTextField(propertyLink, { propertyLink = it }, "Povezava do oglasa (URL)")
                StyledTextField(imageUrl, { imageUrl = it }, "URL slike")
                StyledTextField(source, { source = it }, "Vir (npr. ročno, nepremicnina.si, 24nep.si, generator)")
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
                        propertyType = propertyType.trim(),
                        size = size.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        price = price.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        description = description.ifBlank { null },
                        propertyLink = propertyLink.ifBlank { null },
                        imageUrl = imageUrl.ifBlank { null },
                        source = source.ifBlank { null }
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
