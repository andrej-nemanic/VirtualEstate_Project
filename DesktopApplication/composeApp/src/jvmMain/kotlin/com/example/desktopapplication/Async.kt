package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import com.example.desktopapplication.models.PropertyMapper
import com.example.desktopapplication.models.User
import com.example.desktopapplication.models.UserMapper
import com.example.desktopapplication.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun CoroutineScope.refreshProperties(
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

fun CoroutineScope.refreshUsers(
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

fun CoroutineScope.ingestAll(
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
