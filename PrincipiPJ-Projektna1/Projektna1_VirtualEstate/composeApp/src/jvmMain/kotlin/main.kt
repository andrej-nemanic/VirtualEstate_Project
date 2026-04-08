package org.example.project

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequestBlocking
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Parser

fun main() {

    // Source 1: nepremicnina.si
    println("\n=== nepremicnina.si ===")
    try {
        val doc3: Document = Ksoup.parseGetRequestBlocking(
            url = "https://nepremicnina.si/nepremicnine",
        )

        doc3.select("a[href*=/nepremicnina/]").take(10).forEach { oglas ->
            val lokacija = oglas.select("h1").text()
            val vrsta = oglas.select("h2").text()
            val velikost = oglas.select("h3").text()
            val allText = oglas.text()
            val cena = oglas.ownText().trim().ifEmpty {git
                oglas.select("*").lastOrNull()?.text()?.trim() ?: ""
            }
            val opis = allText
                .substringAfter(velikost, "")
                .substringBefore("Podrobno", "")
                .trim()

            if (lokacija.isNotBlank()) {
                println("$lokacija | $vrsta | $velikost")
                println("  $opis")
                println("  Cena: $cena")
                println()
            }
        }
    } catch (e: Exception) {
        println("Napaka: ${e.message}")
    }

    // Source 2: 24nep.si
    println("\n=== 24nep.si ===")
    try {
        val doc2: Document = Ksoup.parseGetRequestBlocking(
            url = "https://24nep.si/oglasi",
        )

        doc2.select("a[href*=/oglas/]").take(10).forEach { oglas ->
            val lokacija = oglas.select("h2").text()
            val naslov = oglas.select("h4").text()
            val cena = oglas.select("strong").text()
            val vsiP = oglas.select("p")
            val podrobnosti = vsiP.firstOrNull()?.text() ?: ""
            val opis = if (vsiP.size > 1) vsiP.last()?.text()?.take(150) ?: "" else ""

            if (lokacija.isNotBlank()) {
                println("$lokacija | $naslov")
                println("  $podrobnosti")
                if (opis.isNotBlank()) println("  $opis")
                println("  Cena: $cena")
                println()
            }
        }
    } catch (e: Exception) {
        println("Napaka: ${e.message}")
    }

}