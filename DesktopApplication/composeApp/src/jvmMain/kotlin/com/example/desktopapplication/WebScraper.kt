package com.example.desktopapplication

import com.example.desktopapplication.models.Property
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequestBlocking
import com.fleeksoft.ksoup.nodes.Document

object WebScraper {

    private val sizeRegex = Regex("""(\d+(?:[.,]\d+)?)\s*m""")

    private fun parseDouble(text: String): Double {
        val cleaned = text.replace(Regex("[^0-9,.]"), "")
            .replace(".", "")
            .replace(",", ".")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun parseSize(text: String): Double {
        return text.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun parseLocationAlt(alt: String): Triple<String, String, String> {
        val parts = alt.removePrefix("Lokacija:").trim()
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val region = parts.getOrNull(0).orEmpty()
        val city = parts.getOrNull(1).orEmpty()
        val neighborhood = parts.getOrNull(2).orEmpty()
        return Triple(region, city, neighborhood)
    }

    fun scrapeNepremicnina(): List<Property> {
        val results = mutableListOf<Property>()
        try {
            val doc: Document = Ksoup.parseGetRequestBlocking(
                url = "https://nepremicnina.si/nepremicnine"
            )
            doc.select("div.pzl-item.list").forEach { item ->
                val imgAlt = item.selectFirst("img.pzl-gallery-item")?.attr("alt").orEmpty()
                val imgSrc = item.selectFirst("img.pzl-gallery-item")?.attr("src").orEmpty()
                val link = item.selectFirst("a.about")?.attr("href").orEmpty()
                val h2 = item.selectFirst("div.about h2")?.text().orEmpty()
                val h3 = item.selectFirst("div.about h3")?.text().orEmpty()
                val priceText = item.selectFirst("div.price")?.text().orEmpty()
                val descText = item.selectFirst("div.description > div")?.text().orEmpty()
                val badgeText = item.selectFirst("div.badge")?.text().orEmpty()
                val badgeClass = item.selectFirst("div.badge")?.className().orEmpty()

                val (region, city, neighborhood) = parseLocationAlt(imgAlt)

                val propertyType = h2.split(",").firstOrNull()?.trim().orEmpty()

                val offerType = when {
                    badgeText.isNotBlank() -> badgeText
                    badgeClass.contains("to-sell") -> "Prodaja"
                    badgeClass.contains("to-rent") -> "Oddaja"
                    else -> "Prodaja"
                }

                val size = sizeRegex.find(h3)
                    ?.groupValues?.getOrNull(1)
                    ?.let { parseSize(it) } ?: 0.0

                if (city.isNotBlank()) {
                    results.add(
                        Property(
                            region = region,
                            city = city,
                            neighborhood = neighborhood,
                            offerType = offerType,
                            propertyType = propertyType.ifBlank { "Stanovanje" },
                            size = size,
                            price = parseDouble(priceText),
                            description = descText.ifBlank { null },
                            propertyLink = link.ifBlank { null },
                            imageUrl = imgSrc.ifBlank { null }
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
            doc.select("div.pzl-item.list.item").forEach { item ->
                val imgAlt = item.selectFirst("img.pzl-gallery-item")?.attr("alt").orEmpty()
                val imgSrc = item.selectFirst("img.pzl-gallery-item")?.attr("src").orEmpty()
                val link = item.selectFirst("a.data")?.attr("href").orEmpty()
                val h3 = item.selectFirst("div.wrap h3")?.text().orEmpty()
                val h4 = item.selectFirst("div.wrap h4")?.text().orEmpty()
                val propLine = item.selectFirst("div.wrap p")?.text().orEmpty()
                val priceText = item.selectFirst("div.wrap strong")?.text().orEmpty()
                val descText = item.selectFirst("div.wrap div.description")?.text().orEmpty()

                val (region, city, neighborhood) = parseLocationAlt(imgAlt)

                val propertyType = propLine.split("·").firstOrNull()?.trim().orEmpty()
                val offerType = h3.ifBlank { "Prodaja" }

                val size = sizeRegex.find(propLine)
                    ?.groupValues?.getOrNull(1)
                    ?.let { parseSize(it) } ?: 0.0

                val description = listOf(h4, descText).filter { it.isNotBlank() }
                    .joinToString(" — ").ifBlank { null }

                if (city.isNotBlank()) {
                    results.add(
                        Property(
                            region = region,
                            city = city,
                            neighborhood = neighborhood,
                            offerType = offerType,
                            propertyType = propertyType.ifBlank { "Stanovanje" },
                            size = size,
                            price = parseDouble(priceText),
                            description = description,
                            propertyLink = link.ifBlank { null },
                            imageUrl = imgSrc.ifBlank { null }
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
