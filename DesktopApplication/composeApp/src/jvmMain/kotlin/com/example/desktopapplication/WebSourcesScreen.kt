package com.example.desktopapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.desktopapplication.models.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WebSourcesScreen(
    scrapedProperties: List<Property>,
    scrapeErrors: List<String>,
    isScraping: Boolean,
    onScrape: () -> Unit,
    onClearErrors: () -> Unit,
    onSendToDatabase: (List<Property>) -> Unit
) {
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

    val q = filterText.trim()
    val minP = minPrice.toDoubleOrNull()
    val maxP = maxPrice.toDoubleOrNull()
    val minS = minSize.toDoubleOrNull()
    val maxS = maxSize.toDoubleOrNull()
    val result = scrapedProperties.filter { p ->
        val textOk = q.isBlank() || listOf(p.region, p.city, p.neighborhood, p.description.orEmpty(), p.source.orEmpty())
            .any { it.contains(q, ignoreCase = true) }
        val typeOk = filterType.isBlank() || p.propertyType.contains(filterType, ignoreCase = true)
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
        "source" -> result.sortedBy { it.source.orEmpty().lowercase() }
        else -> result
    }
    val filtered = if (sortKey != null && sortDir == SortDirection.DESC) sorted.reversed() else sorted
    val sortClick: (String) -> Unit = { k ->
        val (newKey, newDir) = nextSort(sortKey, sortDir, k); sortKey = newKey; sortDir = newDir
    }
    val totalPages = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val paged = filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

    LaunchedEffect(filterText, filterType, filterOffer, minPrice, maxPrice, minSize, maxSize) {
        selectedIds = emptySet()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton(
                text = if (isScraping) "Nalaganje..." else "Pridobi podatke",
                onClick = onScrape,
                icon = Icons.Default.CloudDownload,
                enabled = !isScraping
            )
            ActionButton(
                text = "Pošlji v bazo (${selectedIds.size})",
                onClick = {
                    val toSend = filtered.filter { it.id in selectedIds }
                    onSendToDatabase(toSend)
                    selectedIds = emptySet()
                },
                color = Color(0xFF27AE60),
                icon = Icons.Default.Save,
                enabled = selectedIds.isNotEmpty()
            )
        }

        if (scrapeErrors.isNotEmpty()) {
            Card(
                elevation = 2.dp,
                backgroundColor = Color(0xFFFDECEA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC0392B))
                            Spacer(Modifier.width(8.dp))
                            Text("Napake pri pridobivanju podatkov:", fontWeight = FontWeight.Bold, color = Color(0xFFC0392B))
                        }
                        TextButton(onClick = onClearErrors) { Text("Skrij") }
                    }
                    scrapeErrors.forEach {
                        Text("• $it", fontSize = 13.sp, color = Color(0xFF7B1F1F))
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyledTextField(filterText, { filterText = it; page = 0 }, "Išči (regija, mesto, opis, vir)", Modifier.weight(2f))
            StyledTextField(filterType, { filterType = it; page = 0 }, "Tip", Modifier.weight(1f))
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
                    val allIds = filtered.mapNotNull { it.id }.toSet()
                    val allSelected = allIds.isNotEmpty() && allIds.all { it in selectedIds }
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + allIds else selectedIds - allIds
                        },
                        modifier = Modifier.weight(0.3f)
                    )
                    SortableHeader("Lokacija", "city", sortKey, sortDir, sortClick, Modifier.weight(2f))
                    SortableHeader("Tip", "propertyType", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    SortableHeader("Ponudba", "offerType", sortKey, sortDir, sortClick, Modifier.weight(0.7f))
                    SortableHeader("Vir", "source", sortKey, sortDir, sortClick, Modifier.weight(0.8f))
                    SortableHeader("m²", "size", sortKey, sortDir, sortClick, Modifier.weight(0.5f))
                    SortableHeader("Cena", "price", sortKey, sortDir, sortClick, Modifier.weight(1f))
                    Text("Opis", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                }
                Divider()
                when {
                    isScraping && scrapedProperties.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    filtered.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(if (scrapedProperties.isEmpty()) "Klikni 'Pridobi podatke' za začetek." else "Filter ne najde zadetkov.", color = Color(0xFF6B7280)) }
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
                                Text(property.source ?: "—", modifier = Modifier.weight(0.8f), color = Color(0xFF6B7280))
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
                    ActionButton("‹ Prejšnja", { page = (currentPage - 1).coerceAtLeast(0) }, enabled = currentPage > 0)
                    ActionButton("Naslednja ›", { page = (currentPage + 1).coerceAtMost(totalPages - 1) }, enabled = currentPage < totalPages - 1)
                }
            }
        }
    }
}
