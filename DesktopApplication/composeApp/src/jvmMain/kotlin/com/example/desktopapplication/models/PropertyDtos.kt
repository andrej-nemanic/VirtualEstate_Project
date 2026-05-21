package com.example.desktopapplication.models

data class PropertyIngestDto(
    val region: String,
    val city: String,
    val neighborhood: String,
    val offerType: String,
    val propertyType: String,
    val size: Double,
    val price: Double,
    val description: String? = null,
    val propertyLink: String? = null,
    val imageUrl: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

data class PropertyResponseDto(
    val _id: String? = null,
    val region: String? = null,
    val city: String? = null,
    val neighborhood: String? = null,
    val offerType: String? = null,
    val propertyType: String? = null,
    val size: Double? = null,
    val price: Double? = null,
    val description: String? = null,
    val propertyLink: String? = null,
    val imageUrl: String? = null
)

object PropertyMapper {

    fun toIngest(
        property: Property,
        lng: Double? = null,
        lat: Double? = null
    ): PropertyIngestDto = PropertyIngestDto(
        region = property.region,
        city = property.city,
        neighborhood = property.neighborhood,
        offerType = property.offerType,
        propertyType = property.propertyType,
        size = property.size,
        price = property.price,
        description = property.description,
        propertyLink = property.propertyLink,
        imageUrl = property.imageUrl,
        lng = lng,
        lat = lat
    )

    fun fromResponse(dto: PropertyResponseDto, localId: Int): Property = Property(
        id = localId,
        apiId = dto._id,
        region = dto.region ?: "",
        city = dto.city ?: "",
        neighborhood = dto.neighborhood ?: "",
        offerType = dto.offerType ?: "Prodaja",
        propertyType = dto.propertyType ?: "",
        size = dto.size ?: 0.0,
        price = dto.price ?: 0.0,
        description = dto.description,
        propertyLink = dto.propertyLink,
        imageUrl = dto.imageUrl
    )
}
