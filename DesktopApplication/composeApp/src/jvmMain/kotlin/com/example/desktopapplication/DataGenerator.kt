package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import kotlin.random.Random

object DataGenerator {

    private data class LocationOption(
        val region: String,
        val city: String,
        val centerLng: Double,
        val centerLat: Double,
        val radiusDeg: Double,
        val neighborhoods: List<String>
    )

    private val locations = listOf(
        LocationOption("Osrednjeslovenska", "Ljubljana", 14.5058, 46.0569, 0.05,
            listOf("Šiška", "Bežigrad", "Center", "Vič", "Moste", "Trnovo", "Rožnik")),
        LocationOption("Podravska", "Maribor", 15.6459, 46.5547, 0.04,
            listOf("Tabor", "Melje", "Pobrežje", "Tezno", "Studenci", "Magdalena")),
        LocationOption("Gorenjska", "Kranj", 14.3554, 46.2389, 0.03,
            listOf("Stražišče", "Zlato Polje", "Planina", "Bitnje")),
        LocationOption("Gorenjska", "Škofja Loka", 14.3077, 46.1676, 0.025, listOf("")),
        LocationOption("Savinjska", "Celje", 15.2675, 46.2311, 0.03, listOf("")),
        LocationOption("Obalno-kraška", "Koper", 13.7295, 45.5466, 0.03, listOf("")),
        LocationOption("Pomurska", "Murska Sobota", 16.1664, 46.6620, 0.025, listOf("")),
        LocationOption("Goriška", "Nova Gorica", 13.6483, 45.9558, 0.03, listOf(""))
    )

    private val propertyTypes = listOf("Stanovanje", "Hiša", "Vikend", "Poslovni prostor", "Garaža", "Parcela")
    private val offerTypes = listOf("Prodaja", "Oddaja")

    private val descAdjectives = listOf(
        "Lepo", "Sodobno", "Prostorno", "Sončno", "Mirno", "Udobno",
        "Prenovljeno", "Ugodno", "Novogradnja", "Klimatizirano"
    )

    private val descFeatures = listOf(
        "balkon", "terasa", "vrt", "garaža", "klet", "parkirno mesto",
        "energetska izkaznica B", "pohodno podstrešje", "dvigalo",
        "lepa lokacija", "mirna ulica", "blizu šole", "blizu trgovine",
        "lep razgled", "talno gretje", "klimatska naprava"
    )

    private val descTrailers = listOf(
        "Idealna naložba.",
        "Primerno za družino.",
        "Vselitev takoj.",
        "Cena po dogovoru.",
        "Ogled po dogovoru.",
        "Več informacij po e-pošti."
    )

    private fun generateDescription(type: String): String {
        val adj = descAdjectives.random()
        val features = descFeatures.shuffled().take(Random.nextInt(2, 4)).joinToString(", ")
        val trailer = descTrailers.random()
        return "$adj ${type.lowercase()}: $features. $trailer"
    }

    fun generate(
        count: Int,
        priceRange: IntRange,
        sizeRange: IntRange
    ): List<Property> = (1..count).map { i ->
        val loc = locations.random()
        val type = propertyTypes.random()
        val jitterLng = (Random.nextDouble() - 0.5) * 2 * loc.radiusDeg
        val jitterLat = (Random.nextDouble() - 0.5) * 2 * loc.radiusDeg
        Property(
            id = i,
            region = loc.region,
            city = loc.city,
            neighborhood = loc.neighborhoods.random(),
            offerType = offerTypes.random(),
            propertyType = type,
            size = Random.nextInt(sizeRange.first, sizeRange.last + 1).toDouble(),
            price = Random.nextInt(priceRange.first, priceRange.last + 1).toDouble(),
            description = generateDescription(type),
            source = "generator",
            lng = loc.centerLng + jitterLng,
            lat = loc.centerLat + jitterLat
        )
    }
}
