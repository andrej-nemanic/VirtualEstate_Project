package src.lexicalAnalyser

class Lexer(private val source: String) {
    private val tokens = mutableListOf<Token>()
    private var start = 0
    private var current = 0
    private var line = 1
    private var column = 1

    // Preslikava ključnih besed iz BNF in realnih primerov
    private val keywords = mapOf(
        "listings" to TokenType.LISTINGS,
        "city" to TokenType.LISTINGS,           // primer -> BNF
        "estate" to TokenType.ESTATE,
        "district" to TokenType.ESTATE,         // primer -> BNF
        "street" to TokenType.ESTATE,           // primer -> BNF
        "landmark" to TokenType.ESTATE,         // primer -> BNF
        "type" to TokenType.TYPE,
        "offerType" to TokenType.OFFER_TYPE,
        "established" to TokenType.OFFER_TYPE,  // primer -> BNF
        "location" to TokenType.LOCATION,
        "price" to TokenType.PRICE,
        "size" to TokenType.SIZE,
        "area" to TokenType.SIZE,               // primer -> BNF
        "length" to TokenType.SIZE,             // primer -> BNF
        "population" to TokenType.SIZE,         // primer -> BNF (obravnavano kot številski izraz)
        "region" to TokenType.REGION,
        "neighborhood" to TokenType.NEIGHBORHOOD,
        "description" to TokenType.DESCRIPTION,
        "source" to TokenType.SOURCE,
        "let" to TokenType.LET,
        "set" to TokenType.SET,
        "parcel" to TokenType.PARCEL,
        "boundary" to TokenType.PARCEL,         // primer -> BNF
        "line" to TokenType.LINE,
        "box" to TokenType.BOX,
        "bend" to TokenType.BEND,
        "fst" to TokenType.FST,
        "snd" to TokenType.SND,
        "true" to TokenType.TRUE,
        "false" to TokenType.FALSE,
        "nil" to TokenType.NIL
    )

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line, column))
        return tokens
    }

    private fun scanToken() {
        val c = advance()
        when (c) {
            '{' -> addToken(TokenType.LBRACE)
            '}' -> addToken(TokenType.RBRACE)
            '(' -> addToken(TokenType.LPAREN)
            ')' -> addToken(TokenType.RPAREN)
            ';' -> addToken(TokenType.SEMICOLON)
            ',' -> addToken(TokenType.COMMA)
            '=' -> addToken(TokenType.ASSIGN)
            '+' -> addToken(TokenType.PLUS)
            '-' -> addToken(TokenType.MINUS)
            '*' -> addToken(TokenType.TIMES)
            '/' -> {
                if (match('/')) {
                    // Komentar traja do konca vrstice
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else {
                    addToken(TokenType.DIVIDE)
                }
            }
            ' ', '\r', '\t' -> { /* Preskoči prazne znake */ }
            '\n' -> {
                line++
                column = 1
            }
            '"' -> stringLiteral()
            else -> {
                if (c.isDigit()) {
                    numberLiteral()
                } else if (c.isLetter() || c == '_') {
                    identifierOrKeyword()
                } else {
                    throw RuntimeException("Leksikalna napaka: Nepričakovan znak '$c' na vrstici $line, stolpec $column")
                }
            }
        }
    }

    private fun identifierOrKeyword() {
        while (peek().isLetterOrDigit() || peek() == '_') advance()
        val text = source.substring(start, current)
        val type = keywords[text] ?: TokenType.IDENTIFIER
        addToken(type)
    }

    private fun numberLiteral() {
        while (peek().isDigit()) advance()

        // Preveri decimalni del
        if (peek() == '.' && peekNext().isDigit()) {
            advance() // Porabi piko "."
            while (peek().isDigit()) advance()
        }

        val text = source.substring(start, current)
        addToken(TokenType.NUMBER, text.toDouble())
    }

    private fun stringLiteral() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++
                column = 1
            }
            advance()
        }

        if (isAtEnd()) {
            throw RuntimeException("Leksikalna napaka: Nedokončan niz na vrstici $line")
        }

        advance() // Zaključni narekovaj "

        // Odstranimo narekovaje iz shranjene vrednosti
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    // Pomožne funkcije za pomikanje po vnosu
    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false
        current++
        column++
        return true
    }

    private fun peek(): Char = if (isAtEnd()) '\u0000' else source[current]

    private fun peekNext(): Char {
        if (current + 1 >= source.length) return '\u0000'
        return source[current + 1]
    }

    private fun advance(): Char {
        val c = source[current++]
        column++
        return c
    }

    private fun isAtEnd(): Boolean = current >= source.length

    private fun addToken(type: TokenType) = addToken(type, null)

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        val tokenColumn = column - (current - start)
        tokens.add(Token(type, text, literal, line, tokenColumn))
    }
}