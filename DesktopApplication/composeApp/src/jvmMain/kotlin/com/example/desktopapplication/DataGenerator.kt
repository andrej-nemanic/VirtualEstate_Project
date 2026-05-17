package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import kotlin.random.Random

object DataGenerator {
    private val cities = listOf("Ljubljana", "Maribor", "Celje", "Kranj", "Koper", "Novo mesto", "Ptuj", "Velenje", "Murska Sobota", "Nova Gorica")
    private val streets = listOf("Slovenska cesta", "Glavni trg", "Cankarjeva ulica", "Trubarjeva cesta", "Dunajska cesta", "Tržaška cesta", "Partizanska cesta", "Prešernova ulica", "Mariborska cesta", "Koroška cesta")
    private val types = listOf("Stanovanje", "Hiša", "Vikend", "Poslovni prostor", "Garaža")

    fun generate(
        count: Int,
        priceRange: IntRange,
        sizeRange: IntRange,
        yearRange: IntRange
    ): List<Property> = (1..count).map { i ->
        Property(
            id = i,
            address = "${streets.random()} ${Random.Default.nextInt(1, 200)}",
            city = cities.random(),
            type = types.random(),
            size = Random.Default.nextInt(sizeRange.first, sizeRange.last + 1).toDouble(),
            price = Random.Default.nextInt(priceRange.first, priceRange.last + 1).toDouble(),
            buildYear = Random.Default.nextInt(yearRange.first, yearRange.last + 1),
            description = "generirano"
        )
    }
}