package org.example.project

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequestBlocking
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Parser

fun main() {

    // Source 1: ARSO for weather data
   val weatherDoc: Document = Ksoup.parseGetRequestBlocking(
       url = "https://meteo.arso.gov.si/uploads/probase/www/observ/surface/text/sl/observation_si_latest.xml",
       parser = Parser.xmlParser()
   )

    // Function saves data with select queries from weaterDoc XML document, then prints it
    println("Weather: ")
    weatherDoc.select("metData").forEach { station: Element ->
        val name = station.select("domain_longTitle").text()
        val temp = station.select("t").text()
        val tempUnit = station.select("t_var_unit").text()
        val shortDescription = station.select("nn_shortText").text()
        println("$name | $temp $tempUnit | $shortDescription")
    }

    // Source 2: Register kulturne dediščine
    val dediDoc: Document = Ksoup.parseGetRequestBlocking(
        url = "https://podatki.gov.si/datastore/dump/aaebf650-5db0-4b5b-9e25-88bf32ff1eeb?format=xml&limit=10",
        parser = Parser.xmlParser(),
    )

    println("\nKulturna dediscina:")
    dediDoc.select("row").forEach { row ->
        val ime = row.select("IME").text()
        val obcina = row.select("OBCINA").text()
        val tip = row.select("TIP").text()
        val datacija = row.select("DATACIJA").text()
        println("$ime | $obcina | $tip | $datacija")
    }
}