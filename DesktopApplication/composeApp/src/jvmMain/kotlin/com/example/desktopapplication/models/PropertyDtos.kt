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

data class PropertyResponseDto(
    val _id: String? = null,
    val address: String? = null,
    val city: String? = null,
    val type: String? = null,
    val size: Double? = null,
    val price: Double? = null,
    val buildYear: Int? = null,
    val description: String? = null
)

object PropertyMapper {

    fun toIngest(
        property: Property,
        lng: Double? = null,
        lat: Double? = null
    ): PropertyIngestDto = PropertyIngestDto(
        address = property.address,
        city = property.city,
        type = property.type,
        size = property.size,
        price = property.price,
        buildYear = property.buildYear,
        description = property.description,
        lng = lng,
        lat = lat
    )

    fun fromResponse(dto: PropertyResponseDto, localId: Int): Property = Property(
        id = localId,
        apiId = dto._id,
        address = dto.address ?: "",
        city = dto.city ?: "",
        type = dto.type ?: "",
        size = dto.size ?: 0.0,
        price = dto.price ?: 0.0,
        buildYear = dto.buildYear ?: 0,
        description = dto.description
    )
}
