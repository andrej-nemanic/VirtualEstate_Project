package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import kotlin.random.Random

object DataGenerator {

    private data class LocationOption(
        val region: String,
        val city: String,
        val neighborhoods: List<String>
    )

    private val locations = listOf(
        LocationOption("Osrednjeslovenska", "Ljubljana", listOf("Šiška", "Bežigrad", "Center", "Vič", "Moste")),
        LocationOption("Podravska", "Maribor", listOf("Tabor", "Melje", "Pobrežje", "Tezno", "Studenci")),
        LocationOption("Gorenjska", "Kranj", listOf("Stražišče", "Zlato Polje", "Planina", "Bitnje")),
        LocationOption("Gorenjska", "Škofja Loka", listOf("")),
        LocationOption("Savinjska", "Celje", listOf("")),
        LocationOption("Obalno-kraška", "Koper", listOf("")),
        LocationOption("Pomurska", "Murska Sobota", listOf("")),
        LocationOption("Goriška", "Nova Gorica", listOf(""))
    )

    private val propertyTypes = listOf("Stanovanje", "Hiša", "Vikend", "Poslovni prostor", "Garaža", "Parcela")
    private val offerTypes = listOf("Prodaja", "Oddaja")

    fun generate(
        count: Int,
        priceRange: IntRange,
        sizeRange: IntRange,
        yearRange: IntRange
    ): List<Property> = (1..count).map { i ->
        val loc = locations.random()
        Property(
            id = i,
            region = loc.region,
            city = loc.city,
            neighborhood = loc.neighborhoods.random(),
            offerType = offerTypes.random(),
            propertyType = propertyTypes.random(),
            size = Random.Default.nextInt(sizeRange.first, sizeRange.last + 1).toDouble(),
            price = Random.Default.nextInt(priceRange.first, priceRange.last + 1).toDouble(),
            description = "Generated"
        )
    }
}
