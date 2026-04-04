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
}