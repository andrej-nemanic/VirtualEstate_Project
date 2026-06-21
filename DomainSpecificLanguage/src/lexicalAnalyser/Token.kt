package src.lexicalAnalyser

enum class TokenType {
    LISTINGS, ESTATE, TYPE, OFFER_TYPE, LOCATION, PRICE, SIZE,
    REGION, NEIGHBORHOOD, DESCRIPTION, SOURCE, LET, SET, PARCEL,
    LINE, BOX, BEND, FST, SND, TRUE, FALSE, NIL,

    IDENTIFIER, STRING, NUMBER,

    LBRACE, RBRACE, LPAREN, RPAREN, SEMICOLON, COMMA, ASSIGN,
    PLUS, MINUS, TIMES, DIVIDE,

    EOF, IGNORE
}

val tokenFormatNames: Map<TokenType, String> = mapOf(
    TokenType.LISTINGS to "listings",
    TokenType.ESTATE to "estate",
    TokenType.TYPE to "type",
    TokenType.OFFER_TYPE to "offerType",
    TokenType.LOCATION to "location",
    TokenType.PRICE to "price",
    TokenType.SIZE to "size",
    TokenType.REGION to "region",
    TokenType.NEIGHBORHOOD to "neighborhood",
    TokenType.DESCRIPTION to "description",
    TokenType.SOURCE to "source",
    TokenType.LET to "let",
    TokenType.SET to "set",
    TokenType.PARCEL to "parcel",
    TokenType.LINE to "line",
    TokenType.BOX to "box",
    TokenType.BEND to "bend",
    TokenType.FST to "fst",
    TokenType.SND to "snd",
    TokenType.TRUE to "true",
    TokenType.FALSE to "false",
    TokenType.NIL to "nil",
    TokenType.IDENTIFIER to "identifier",
    TokenType.STRING to "string",
    TokenType.NUMBER to "number",
    TokenType.LBRACE to "lbrace",
    TokenType.RBRACE to "rbrace",
    TokenType.LPAREN to "lparen",
    TokenType.RPAREN to "rparen",
    TokenType.SEMICOLON to "semicolon",
    TokenType.COMMA to "comma",
    TokenType.ASSIGN to "assign",
    TokenType.PLUS to "plus",
    TokenType.MINUS to "minus",
    TokenType.TIMES to "times",
    TokenType.DIVIDE to "divide"
)

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
