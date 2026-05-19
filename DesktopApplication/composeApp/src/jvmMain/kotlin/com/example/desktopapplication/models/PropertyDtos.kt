package com.example.desktopapplication.models

data class PropertyIngestDto(
    val address: String,
    val city: String,
    val type: String,
    val size: Double,
    val price: Double,
    val buildYear: Int,
    val description: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

data class LocationResponseDto(
    val _id: String? = null,
    val address: String? = null,
    val city: String? = null
)

data class PropertyResponseDto(
    val _id: String? = null,
    val location: LocationResponseDto? = null,
    val type: String? = null,
    val size: Double? = null,
    val price: Double? = null,
    val buildYear: Int? = null,
    val description: String? = null
)

object PropertyMapper {

    private val toBackend = mapOf(
        "stanovanje" to "apartment",
        "apartment" to "apartment",
        "apartma" to "apartment",
        "vikend" to "apartment",
        "poslovni prostor" to "apartment",
        "garaža" to "apartment",
        "garaza" to "apartment",
        "hiša" to "house",
        "hisa" to "house",
        "house" to "house",
        "zemljišče" to "land",
        "zemljisce" to "land",
        "land" to "land",
        "condominium" to "condominium"
    )

    private val toDisplay = mapOf(
        "apartment" to "Stanovanje",
        "house" to "Hiša",
        "land" to "Zemljišče",
        "condominium" to "Apartma"
    )

    fun normalizeForBackend(type: String?): String =
        toBackend[type?.trim()?.lowercase()] ?: "house"

    fun displayFromBackend(type: String?): String =
        toDisplay[type?.trim()?.lowercase()] ?: (type ?: "")

    fun toIngest(property: Property): PropertyIngestDto = PropertyIngestDto(
        address = property.address,
        city = property.city,
        type = normalizeForBackend(property.type),
        size = property.size,
        price = property.price,
        buildYear = property.buildYear,
        description = property.description
    )

    fun fromResponse(dto: PropertyResponseDto, localId: Int): Property = Property(
        id = localId,
        apiId = dto._id,
        address = dto.location?.address ?: "",
        city = dto.location?.city ?: "",
        type = displayFromBackend(dto.type),
        size = dto.size ?: 0.0,
        price = dto.price ?: 0.0,
        buildYear = dto.buildYear ?: 0,
        description = dto.description
    )
}
