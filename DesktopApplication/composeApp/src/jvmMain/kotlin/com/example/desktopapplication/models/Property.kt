package com.example.desktopapplication.models

import kotlinx.serialization.Serializable

@Serializable
data class Nepremicnina(
    val id: Int? = null,
    val address: String,
    val city: String,
    val type: String,
    val size: Double,
    val price: Double,
    val buildYear: Int,
    val description: String? = null
)