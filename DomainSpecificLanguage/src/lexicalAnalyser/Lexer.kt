package src.lexicalAnalyser

class Lexer(private val source: String) {
    private var cursor = 0
    private var line = 1
    private var column = 1
    private val tokens = mutableListOf<Token>()

    companion object {
        private const val TABLE_WIDTH = 0x180
        private const val NO_EDGE = -1

        private val automata = ArrayList<IntArray>()
        private val finite = ArrayList<TokenType?>()

        private val START: Int
        private val IDENT: Int
        private val identChars: IntArray

        private fun newState(): Int {
            automata.add(IntArray(TABLE_WIDTH) { NO_EDGE })
            finite.add(null)
            return automata.size - 1
        }

        private fun isLetterCode(c: Int): Boolean = when {
            c in 'a'.code..'z'.code -> true
            c in 'A'.code..'Z'.code -> true
            c == 0xD7 || c == 0xF7 -> false
            c in 0xC0..0x17F -> true
            else -> false
        }

        private fun addIdentFallback(state: Int) {
            for (c in identChars) automata[state][c] = IDENT
        }

        private fun addKeyword(word: String, type: TokenType) {
            var state = START
            for (ch in word) {
                val code = ch.code
                val next = automata[state][code]
                state = if (next == NO_EDGE || next == IDENT) {
                    val node = newState()
                    finite[node] = TokenType.IDENTIFIER
                    addIdentFallback(node)
                    automata[state][code] = node
                    node
                } else {
                    next
                }
            }
            finite[state] = type
        }

        init {
            identChars = (0 until TABLE_WIDTH).filter {
                isLetterCode(it) || it in '0'.code..'9'.code || it == '_'.code
            }.toIntArray()

            START = newState()
            IDENT = newState()

            for (c in 0 until TABLE_WIDTH) {
                if (isLetterCode(c) || c == '_'.code) automata[START][c] = IDENT
            }
            finite[IDENT] = TokenType.IDENTIFIER
            addIdentFallback(IDENT)

            addKeyword("listings", TokenType.LISTINGS)
            addKeyword("estate", TokenType.ESTATE)
            addKeyword("parcel", TokenType.PARCEL)
            addKeyword("let", TokenType.LET)
            addKeyword("type", TokenType.TYPE)
            addKeyword("offerType", TokenType.OFFER_TYPE)
            addKeyword("location", TokenType.LOCATION)
            addKeyword("price", TokenType.PRICE)
            addKeyword("size", TokenType.SIZE)
            addKeyword("region", TokenType.REGION)
            addKeyword("neighborhood", TokenType.NEIGHBORHOOD)
            addKeyword("description", TokenType.DESCRIPTION)
            addKeyword("source", TokenType.SOURCE)
            addKeyword("set", TokenType.SET)
            addKeyword("line", TokenType.LINE)
            addKeyword("box", TokenType.BOX)
            addKeyword("bend", TokenType.BEND)
            addKeyword("fst", TokenType.FST)
            addKeyword("snd", TokenType.SND)
            addKeyword("true", TokenType.TRUE)
            addKeyword("false", TokenType.FALSE)
            addKeyword("nil", TokenType.NIL)

            val intState = newState()
            val dotState = newState()
            val fracState = newState()
            for (c in '0'.code..'9'.code) {
                automata[START][c] = intState
                automata[intState][c] = intState
                automata[dotState][c] = fracState
                automata[fracState][c] = fracState
            }
            automata[intState]['.'.code] = dotState
            finite[intState] = TokenType.NUMBER
            finite[fracState] = TokenType.NUMBER

            val strOpen = newState()
            val strClose = newState()
            automata[START]['"'.code] = strOpen
            for (c in 0 until TABLE_WIDTH) {
                if (c != '"'.code && c != '\n'.code && c != '\r'.code)
                    automata[strOpen][c] = strOpen
            }
            automata[strOpen]['"'.code] = strClose
            finite[strClose] = TokenType.STRING

            fun single(ch: Char, type: TokenType) {
                val s = newState()
                automata[START][ch.code] = s
                finite[s] = type
            }
            single('{', TokenType.LBRACE)
            single('}', TokenType.RBRACE)
            single('(', TokenType.LPAREN)
            single(')', TokenType.RPAREN)
            single(',', TokenType.COMMA)
            single(';', TokenType.SEMICOLON)
            single('=', TokenType.ASSIGN)
            single('+', TokenType.PLUS)
            single('-', TokenType.MINUS)
            single('*', TokenType.TIMES)

            val divideState = newState()
            val commentState = newState()
            automata[START]['/'.code] = divideState
            finite[divideState] = TokenType.DIVIDE
            automata[divideState]['/'.code] = commentState
            for (c in 0 until TABLE_WIDTH) {
                if (c != '\n'.code && c != '\r'.code) automata[commentState][c] = commentState
            }
            finite[commentState] = TokenType.IGNORE

            val wsState = newState()
            for (ws in intArrayOf(' '.code, '\t'.code, '\n'.code, '\r'.code)) {
                automata[START][ws] = wsState
                automata[wsState][ws] = wsState
            }
            finite[wsState] = TokenType.IGNORE
        }
    }

    private fun advance(): Char {
        val c = source[cursor++]
        if (c == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return c
    }

    private fun nextToken(): Token {
        while (true) {
            var state = START
            val sb = StringBuilder()
            val tokLine = line
            val tokCol = column

            while (true) {
                val p = if (cursor < source.length) source[cursor].code else -1
                val next = if (p in 0 until TABLE_WIDTH) automata[state][p] else NO_EDGE

                if (next != NO_EDGE) {
                    state = next
                    sb.append(advance())
                    continue
                }

                val type = finite[state]
                when {
                    type == TokenType.IGNORE -> { }
                    type != null -> return makeToken(type, sb.toString(), tokLine, tokCol)
                    state == START && cursor >= source.length ->
                        return Token(TokenType.EOF, "", null, tokLine, tokCol)
                    else -> {
                        if (cursor < source.length) {
                            val bad = source[cursor]
                            System.err.println(
                                "Lexical error: unexpected character '$bad' (code ${bad.code}) " +
                                    "at line $line, column $column"
                            )
                            advance()
                        } else {
                            System.err.println(
                                "Lexical error: unterminated token '$sb' at line $tokLine, column $tokCol"
                            )
                        }
                    }
                }
                break
            }
        }
    }

    private fun makeToken(type: TokenType, lexeme: String, l: Int, c: Int): Token {
        val literal: Any? = when (type) {
            TokenType.NUMBER -> lexeme.toDouble()
            TokenType.STRING -> lexeme.substring(1, lexeme.length - 1)
            else -> null
        }
        return Token(type, lexeme, literal, l, c)
    }

    fun scanTokens(): List<Token> {
        while (true) {
            val t = nextToken()
            tokens.add(t)
            if (t.type == TokenType.EOF) break
        }
        return tokens
    }
}
