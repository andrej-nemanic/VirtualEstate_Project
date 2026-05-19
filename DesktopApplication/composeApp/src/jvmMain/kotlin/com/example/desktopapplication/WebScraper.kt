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

    private fun parseSize(text: String): Double {
        return text.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
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
            doc.select("div.pzl-item.list").forEach { item ->
                val imgAlt = item.selectFirst("img.pzl-gallery-item")?.attr("alt").orEmpty()
                val h2 = item.selectFirst("div.about h2")?.text().orEmpty()
                val h3 = item.selectFirst("div.about h3")?.text().orEmpty()
                val priceText = item.selectFirst("div.price")?.text().orEmpty()
                val descText = item.selectFirst("div.description > div")?.text().orEmpty()

                val altParts = imgAlt.removePrefix("Lokacija:").trim()
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val city = altParts.getOrNull(1).orEmpty()
                val district = altParts.getOrNull(2).orEmpty()
                val address = if (district.isNotEmpty() && district != city)
                    "$district, $city" else city

                val propType = h2.split(",").firstOrNull()?.trim().orEmpty()
                val size = sizeRegex.find(h3)
                    ?.groupValues?.getOrNull(1)
                    ?.let { parseSize(it) } ?: 0.0

                val description = descText.ifBlank { "nepremicnina.si" }

                if (city.isNotBlank()) {
                    results.add(
                        Property(
                            address = address,
                            city = city,
                            type = propType.ifBlank { "Stanovanje" },
                            size = size,
                            price = parseDouble(priceText),
                            buildYear = 0,
                            description = description
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Error nepremicnina.si: ${e.message}")
        }
        return results
    }

    private val sizeRegex = Regex("""(\d+(?:[.,]\d+)?)\s*m""")

    fun scrape24Nep(): List<Property> {
        val results = mutableListOf<Property>()
        try {
            val doc: Document = Ksoup.parseGetRequestBlocking(
                url = "https://24nep.si/oglasi"
            )
            doc.select("div.pzl-item.list.item").forEach { item ->
                val h2 = item.selectFirst("h2")?.text().orEmpty()
                val h4 = item.selectFirst("h4")?.text().orEmpty()
                val propLine = item.selectFirst("div.wrap p")?.text().orEmpty()
                val priceText = item.selectFirst("strong")?.text().orEmpty()
                val descText = item.selectFirst("div.description")?.text().orEmpty()
                val imgAlt = item.selectFirst("img.pzl-gallery-item")?.attr("alt").orEmpty()

                val altParts = imgAlt.removePrefix("Lokacija:").trim()
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val city = altParts.getOrNull(1) ?: extractCity(h2)
                val neighborhood = altParts.getOrNull(2).orEmpty()
                val address = if (neighborhood.isNotEmpty() && neighborhood != city)
                    "$neighborhood, $city" else city

                val propType = propLine.split("·").firstOrNull()?.trim().orEmpty()
                val size = sizeRegex.find(propLine)
                    ?.groupValues?.getOrNull(1)
                    ?.let { parseSize(it) } ?: 0.0

                val description = listOf(h4, descText).filter { it.isNotBlank() }
                    .joinToString(" — ").ifBlank { "24nep.si" }

                if (city.isNotBlank()) {
                    results.add(
                        Property(
                            address = address,
                            city = city,
                            type = propType.ifBlank { "Stanovanje" },
                            size = size,
                            price = parseDouble(priceText),
                            buildYear = 0,
                            description = description
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