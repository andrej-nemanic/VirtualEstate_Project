package src
import src.lexicalAnalyser.Lexer

fun main() {
    val sampleInput = """
        city "Novo Mesto" {
          let center_x = 15.1651;
          let center_y = 45.8021;
          let center = (center_x, center_y);
        }
    """.trimIndent()

    try {
        val lexer = Lexer(sampleInput)
        val tokens = lexer.scanTokens()

        println("--- Uspešno prepoznani žetoni ---")
        tokens.forEach { println(it) }
    } catch (e: Exception) {
        println(e.message)
    }
}