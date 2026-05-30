package src.syntaxAnalyser

import src.lexicalAnalyser.Token
import src.lexicalAnalyser.TokenType
import src.lexicalAnalyser.TokenType.*

class ParseException(message: String) : RuntimeException(message)

class Parser(private val tokens: List<Token>) {
    private var pos = 0

    fun parse(): Program {
        val listings = parseListings()
        expect(EOF)
        return Program(listings)
    }

    private fun parseListings(): Listings {
        expect(LISTINGS)
        val name = expect(STRING).literal as String
        expect(LBRACE)
        val body = ArrayList<ListingsStmt>()
        while (!check(RBRACE) && !isAtEnd()) body.add(parseListingsStmt())
        expect(RBRACE)
        return Listings(name, body)
    }

    private fun parseListingsStmt(): ListingsStmt = when (peek().type) {
        LET -> parseLet()
        ESTATE -> parseEstate()
        else -> err("'let' or 'estate'")
    }

    private fun parseEstate(): EstateDecl {
        expect(ESTATE)
        val name = expect(STRING).literal as String
        expect(LBRACE)
        val body = ArrayList<EstateStmt>()
        while (!check(RBRACE) && !isAtEnd()) body.add(parseEstateStmt())
        expect(RBRACE)
        return EstateDecl(name, body)
    }

    private fun parseEstateStmt(): EstateStmt = when (peek().type) {
        TYPE -> { advance(); val v = expect(IDENTIFIER).lexeme; expect(SEMICOLON); TypeStmt(v) }
        OFFER_TYPE -> { advance(); val v = expect(IDENTIFIER).lexeme; expect(SEMICOLON); OfferTypeStmt(v) }
        LOCATION -> { advance(); val p = parsePointExpr(); expect(SEMICOLON); LocationStmt(p) }
        PRICE -> { advance(); val e = parseExpr(); expect(SEMICOLON); PriceStmt(e) }
        SIZE -> { advance(); val e = parseExpr(); expect(SEMICOLON); SizeStmt(e) }
        REGION -> { advance(); val v = expect(STRING).literal as String; expect(SEMICOLON); RegionStmt(v) }
        NEIGHBORHOOD -> { advance(); val v = expect(STRING).literal as String; expect(SEMICOLON); NeighborhoodStmt(v) }
        DESCRIPTION -> { advance(); val v = expect(STRING).literal as String; expect(SEMICOLON); DescriptionStmt(v) }
        SOURCE -> { advance(); val v = expect(STRING).literal as String; expect(SEMICOLON); SourceStmt(v) }
        SET -> parseSet()
        PARCEL -> parseParcel()
        LET -> parseLet()
        else -> err("estate property, set, parcel or let")
    }

    private fun parseLet(): LetStmt {
        expect(LET)
        val name = expect(IDENTIFIER).lexeme
        expect(ASSIGN)
        val value = parseValue()
        expect(SEMICOLON)
        return LetStmt(name, value)
    }

    private fun parseSet(): SetStmt {
        expect(SET)
        expect(LPAREN)
        val key = expect(STRING).literal as String
        expect(COMMA)
        val value = parseValue()
        expect(RPAREN)
        expect(SEMICOLON)
        return SetStmt(key, value)
    }

    private fun parseParcel(): ParcelDecl {
        expect(PARCEL)
        expect(LBRACE)
        val cmds = ArrayList<GeoCommand>()
        if (!check(RBRACE) && !isAtEnd()) {
            cmds.add(parseGeoCommand())
            while (check(SEMICOLON)) {
                advance()
                if (check(RBRACE) || isAtEnd()) break
                cmds.add(parseGeoCommand())
            }
        }
        expect(RBRACE)
        return ParcelDecl(cmds)
    }

    private fun parseGeoCommand(): GeoCommand = when (peek().type) {
        LINE -> {
            advance(); expect(LPAREN)
            val a = parsePointExpr(); expect(COMMA); val b = parsePointExpr()
            expect(RPAREN); LineCmd(a, b)
        }
        BOX -> {
            advance(); expect(LPAREN)
            val a = parsePointExpr(); expect(COMMA); val b = parsePointExpr()
            expect(RPAREN); BoxCmd(a, b)
        }
        BEND -> {
            advance(); expect(LPAREN)
            val a = parsePointExpr(); expect(COMMA); val b = parsePointExpr()
            expect(COMMA); val ang = parseExpr()
            expect(RPAREN); BendCmd(a, b, ang)
        }
        else -> err("geometric command (line, box or bend)")
    }

    private fun parsePointExpr(): Expr = when (peek().type) {
        LPAREN -> parsePoint()
        IDENTIFIER -> VarRef(advance().lexeme)
        else -> err("point (x, y) or identifier")
    }

    private fun parsePoint(): PointLit {
        expect(LPAREN)
        val x = parseExpr()
        expect(COMMA)
        val y = parseExpr()
        expect(RPAREN)
        return PointLit(x, y)
    }

    private fun parseValue(): Expr {
        if (check(LPAREN) && isPointAhead()) return parsePoint()
        return parseExpr()
    }

    private fun isPointAhead(): Boolean {
        var depth = 0
        var i = pos
        while (i < tokens.size) {
            when (tokens[i].type) {
                LPAREN -> depth++
                RPAREN -> { depth--; if (depth == 0) return false }
                COMMA -> if (depth == 1) return true
                EOF -> return false
                else -> {}
            }
            i++
        }
        return false
    }

    private fun parseExpr(): Expr = parseAdditive()

    private fun parseAdditive(): Expr {
        var left = parseMultiplicative()
        while (check(PLUS) || check(MINUS)) {
            val op = advance().lexeme
            val right = parseMultiplicative()
            left = Binary(op, left, right)
        }
        return left
    }

    private fun parseMultiplicative(): Expr {
        var left = parseUnary()
        while (check(TIMES) || check(DIVIDE)) {
            val op = advance().lexeme
            val right = parseUnary()
            left = Binary(op, left, right)
        }
        return left
    }

    private fun parseUnary(): Expr {
        if (check(MINUS) || check(PLUS)) {
            val op = advance().lexeme
            return Unary(op, parsePrimary())
        }
        return parsePrimary()
    }

    private fun parsePrimary(): Expr = when (peek().type) {
        NUMBER -> NumberLit(advance().literal as Double)
        STRING -> StringLit(advance().literal as String)
        IDENTIFIER -> VarRef(advance().lexeme)
        TRUE -> { advance(); BoolLit(true) }
        FALSE -> { advance(); BoolLit(false) }
        NIL -> { advance(); NilLit }
        FST -> { advance(); expect(LPAREN); val e = parseExpr(); expect(RPAREN); FstExpr(e) }
        SND -> { advance(); expect(LPAREN); val e = parseExpr(); expect(RPAREN); SndExpr(e) }
        LPAREN -> { advance(); val e = parseExpr(); expect(RPAREN); e }
        else -> err("expression")
    }

    private fun peek(): Token = tokens[pos]
    private fun check(type: TokenType): Boolean = peek().type == type
    private fun isAtEnd(): Boolean = peek().type == EOF

    private fun advance(): Token {
        val t = tokens[pos]
        if (t.type != EOF) pos++
        return t
    }

    private fun expect(type: TokenType): Token {
        if (check(type)) return advance()
        return err("'$type'")
    }

    private fun err(expected: String): Nothing {
        val t = peek()
        throw ParseException(
            "Syntax error: expected $expected, got '${t.lexeme}' (${t.type}) " +
                "at line ${t.line}, column ${t.column}"
        )
    }
}
