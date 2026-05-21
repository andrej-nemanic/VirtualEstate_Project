package com.example.desktopapplication.models

import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val id: Int? = null,
    val apiId: String? = null,
    val region: String = "",
    val city: String,
    val neighborhood: String = "",
    val offerType: String = "Prodaja",
    val propertyType: String,
    val size: Double,
    val price: Double,
    val description: String? = null,
    val propertyLink: String? = null,
    val imageUrl: String? = null
)
