package src

import src.geojson.GeoJsonGenerator
import src.lexicalAnalyser.Lexer
import src.lexicalAnalyser.TokenType
import src.lexicalAnalyser.tokenFormatNames
import src.semantics.Validator
import src.syntaxAnalyser.AstPrinter
import src.syntaxAnalyser.Parser
import java.io.File

fun main(args: Array<String>) {
    val printTokens = args.contains("--tokens")
    val printAst = args.contains("--ast")
    val fileArg = args.firstOrNull { !it.startsWith("--") }

    val source: String = if (fileArg != null) {
        val file = File(fileArg)
        if (!file.exists()) {
            System.err.println("Error: no such file: '$fileArg'")
            return
        }
        file.readText()
    } else {
        SAMPLE
    }

    val tokens = Lexer(source).scanTokens()

    if (printTokens) {
        System.err.println("--- Tokens ---")
        tokens.filter { it.type != TokenType.EOF }.forEach { token ->
            val name = tokenFormatNames[token.type] ?: token.type.name.lowercase()
            val text = if (token.type == TokenType.STRING) token.literal as String else token.lexeme
            System.err.println("$name(\"$text\")")
        }
        System.err.println("--- GeoJSON ---")
    }

    try {
        val program = Parser(tokens).parse()

        if (printAst) {
            System.err.println("--- AST ---")
            System.err.println(AstPrinter.print(program))
        }

        val issues = Validator().validate(program)
        if (issues.isNotEmpty()) {
            System.err.println("--- Validation ---")
            issues.forEach { System.err.println(it) }
        }

        val geoJson = GeoJsonGenerator().generate(program)
        println(geoJson)
    } catch (e: Exception) {
        System.err.println(e.message)
    }
}

private val SAMPLE = """
    listings "Maribor" {
      let center = (15.6467, 46.5547);
      let m2price = 2500;

      estate "Apartment Center" {
        type stanovanje;
        offerType prodaja;
        location center;
        size 65;
        price m2price * 65;
        region "Podravska";
        neighborhood "Center";
        description "Apartment in the city center";
        source "nepremicnine.si";
        set("heating", "gas");

        parcel {
          line ((15.645, 46.553), (15.648, 46.553));
          line ((15.648, 46.553), (15.648, 46.556));
          bend ((15.648, 46.556), (15.645, 46.556), 20);
          line ((15.645, 46.556), (15.645, 46.553))
        }
      }

      estate "House Tabor" {
        type hiša;
        offerType najem;
        location (fst(center) + 0.01, snd(center) - 0.02);
        size 120;
        price m2price * 120 - 5000;
        region "Podravska";
        neighborhood "Tabor";
        description "House with a garden";
        source "24nep.si";
        set("built", 1998);

        parcel {
          box ((15.652, 46.548), (15.654, 46.546))
        }
      }
    }
""".trimIndent()
