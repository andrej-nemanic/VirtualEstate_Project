package org.example.project

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequestBlocking
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements

fun main() {
    val doc: Document = Ksoup.parseGetRequestBlocking(url = "https://slo-tech.com/")
    val allNews: Elements = doc.select(".news_item")

    allNews.forEach { singleNews ->
        val title = singleNews.select("h3 a").text()
        val content = singleNews.select(".besediloNovice").text()

        println("[$title] $content\n")
    }
}