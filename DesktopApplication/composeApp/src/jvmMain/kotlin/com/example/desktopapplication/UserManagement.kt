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
import com.example.desktopapplication.models.User

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
    val filtered = if (sortKey != null && sortDir == SortDirection.DESC) sorted.reversed() else sorted
    val sortClick: (String) -> Unit = { k ->
        val (newKey, newDir) = nextSort(sortKey, sortDir, k); sortKey = newKey; sortDir = newDir
    }
    val totalPages = ((filtered.size - 1) / PAGE_SIZE + 1).coerceAtLeast(1)
    val currentPage = page.coerceIn(0, totalPages - 1)
    val paged = filtered.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)
    val selectedItems = items.filter { (it.apiId ?: "") in selectedApiIds }

    LaunchedEffect(filter, adminFilter) { selectedApiIds = emptySet() }

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
                    val allApiIds = filtered.mapNotNull { it.apiId }.toSet()
                    val allSelected = allApiIds.isNotEmpty() && allApiIds.all { it in selectedApiIds }
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            selectedApiIds = if (checked) selectedApiIds + allApiIds else selectedApiIds - allApiIds
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
