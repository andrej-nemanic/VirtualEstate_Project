package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import kotlin.random.Random

object DataGenerator {

    private data class LocationOption(
        val region: String,
        val city: String,
        val centerLng: Double,
        val centerLat: Double,
        val neighborhoods: List<String>
    )

    private val locations = listOf(
        LocationOption("Osrednjeslovenska", "Ljubljana", 14.5058, 46.0569,
            listOf("Šiška", "Bežigrad", "Center", "Vič", "Moste")),
        LocationOption("Podravska", "Maribor", 15.6459, 46.5547,
            listOf("Tabor", "Melje", "Pobrežje", "Tezno", "Studenci")),
        LocationOption("Gorenjska", "Kranj", 14.3554, 46.2389,
            listOf("Stražišče", "Zlato Polje", "Planina", "Bitnje")),
        LocationOption("Gorenjska", "Škofja Loka", 14.3077, 46.1676, listOf("")),
        LocationOption("Savinjska", "Celje", 15.2675, 46.2311, listOf("")),
        LocationOption("Obalno-kraška", "Koper", 13.7295, 45.5466, listOf("")),
        LocationOption("Pomurska", "Murska Sobota", 16.1664, 46.6620, listOf("")),
        LocationOption("Goriška", "Nova Gorica", 13.6483, 45.9558, listOf(""))
    )

    private val propertyTypes = listOf("Stanovanje", "Hiša", "Vikend", "Poslovni prostor", "Garaža", "Parcela")
    private val offerTypes = listOf("Prodaja", "Oddaja")

    fun generate(
        count: Int,
        priceRange: IntRange,
        sizeRange: IntRange
    ): List<Property> = (1..count).map { i ->
        val loc = locations.random()
        val jitterLng = (Random.Default.nextDouble() - 0.5) * 0.02
        val jitterLat = (Random.Default.nextDouble() - 0.5) * 0.02
        Property(
            id = i,
            region = loc.region,
            city = loc.city,
            neighborhood = loc.neighborhoods.random(),
            offerType = offerTypes.random(),
            propertyType = propertyTypes.random(),
            size = Random.Default.nextInt(sizeRange.first, sizeRange.last + 1).toDouble(),
            price = Random.Default.nextInt(priceRange.first, priceRange.last + 1).toDouble(),
            description = "Generated",
            lng = loc.centerLng + jitterLng,
            lat = loc.centerLat + jitterLat
        )
    }
}
