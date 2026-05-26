package com.example.desktopapplication.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException

private val gson = Gson()

private data class ApiErrorBody(val message: String? = null)

fun extractApiErrorMessage(t: Throwable, fallback: String = "Napaka pri klicu strežnika"): String {
    if (t is HttpException) {
        val response = t.response()
        val body = response?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            try {
                val parsed = gson.fromJson(body, ApiErrorBody::class.java)
                val msg = parsed?.message
                if (!msg.isNullOrBlank()) return msg
            } catch (_: JsonSyntaxException) {
                // ignoriraj, vrnemo HTTP kratko sporočilo
            }
        }
        val code = t.code()
        val statusReason = when (code) {
            400 -> "Neveljaven vnos"
            401 -> "Niste prijavljeni"
            403 -> "Brez dovoljenja"
            404 -> "Ni najdeno"
            409 -> "Zapis že obstaja"
            429 -> "Preveč zahtev, poskusi kasneje"
            500 -> "Notranja napaka strežnika"
            else -> "HTTP $code"
        }
        return statusReason
    }
    return t.message ?: fallback
}
