package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequestBlocking
import com.fleeksoft.ksoup.nodes.Document

object WebScraper {

    private fun parseDouble(text: String): Double {
        val cleaned = text.replace(Regex("[^0-9,.]"), "")
            .replace(".", "")
            .replace(",", ".")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun extractCity(location: String): String {
        return location.split(",").lastOrNull()?.trim() ?: location
    }

    fun scrapeNepremicnina(): List<Property> {
        val results = mutableListOf<Property>()
        try {
            val doc: Document = Ksoup.parseGetRequestBlocking(
                url = "https://nepremicnina.si/nepremicnine"
            )
            doc.select("a[href*=/nepremicnina/]").take(10).forEach { listing ->
                val location = listing.select("h1").text()
                val propertyType = listing.select("h2").text()
                val sizeText = listing.select("h3").text()
                val priceText = listing.ownText().trim().ifEmpty {
                    listing.select("*").lastOrNull()?.text()?.trim() ?: ""
                }

                if (location.isNotBlank()) {
                    results.add(
                        Property(
                            address = location,
                            city = extractCity(location),
                            type = propertyType.ifBlank { "Apartment" },
                            size = parseDouble(sizeText),
                            price = parseDouble(priceText),
                            buildYear = 0,
                            description = "nepremicnina.si"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Error nepremicnina.si: ${e.message}")
        }
        return results
    }

    fun scrape24Nep(): List<Property> {
        val results = mutableListOf<Property>()
        try {
            val doc: Document = Ksoup.parseGetRequestBlocking(
                url = "https://24nep.si/oglasi"
            )
            doc.select("a[href*=/oglas/]").take(10).forEach { listing ->
                val location = listing.select("h2").text()
                val title = listing.select("h4").text()
                val priceText = listing.select("strong").text()
                val paragraphs = listing.select("p")
                val details = paragraphs.firstOrNull()?.text() ?: ""

                if (location.isNotBlank()) {
                    results.add(
                        Property(
                            address = title.ifBlank { location },
                            city = extractCity(location),
                            type = "Apartment",
                            size = parseDouble(details),
                            price = parseDouble(priceText),
                            buildYear = 0,
                            description = "24nep.si"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Error 24nep.si: ${e.message}")
        }
        return results
    }

    fun scrapeAll(): List<Property> {
        return scrapeNepremicnina() + scrape24Nep()
    }
}