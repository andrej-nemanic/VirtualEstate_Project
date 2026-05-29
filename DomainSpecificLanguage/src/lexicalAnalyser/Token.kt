package src.lexicalAnalyser
enum class TokenType {
    // Ključne besede iz BNF (in njihovi ekvivalenti iz primerov)
    LISTINGS, ESTATE, TYPE, OFFER_TYPE, LOCATION, PRICE, SIZE,
    REGION, NEIGHBORHOOD, DESCRIPTION, SOURCE, LET, SET, PARCEL,
    LINE, BOX, BEND, FST, SND, TRUE, FALSE, NIL,

    // Literali
    IDENTIFIER, STRING, NUMBER,

    // Ločila in operatorji
    LBRACE, RBRACE, LPAREN, RPAREN, SEMICOLON, COMMA, ASSIGN,
    PLUS, MINUS, TIMES, DIVIDE,

    // Konec datoteke
    EOF
}
data class Token(
    val type: TokenType,
    val lexeme: String,
    val literal: Any?,
    val line: Int,
    val column: Int
) {
    override fun toString(): String {
        return "Token(type=$type, lexeme='$lexeme', literal=$literal, line=$line, col=$column)"
    }
}