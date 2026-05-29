package src.syntaxAnalyser

object AstPrinter {

    fun print(program: Program): String {
        val sb = StringBuilder()
        line(sb, 0, "Program")
        listings(sb, program.listings, 1)
        return sb.toString().trimEnd()
    }

    private fun line(sb: StringBuilder, depth: Int, text: String) {
        repeat(depth) { sb.append("  ") }
        sb.append(text).append('\n')
    }

    private fun listings(sb: StringBuilder, l: Listings, d: Int) {
        line(sb, d, "Listings \"${l.name}\"")
        for (s in l.body) listingsStmt(sb, s, d + 1)
    }

    private fun listingsStmt(sb: StringBuilder, s: ListingsStmt, d: Int) = when (s) {
        is LetStmt -> line(sb, d, "Let ${s.name} = ${expr(s.value)}")
        is EstateDecl -> estate(sb, s, d)
    }

    private fun estate(sb: StringBuilder, e: EstateDecl, d: Int) {
        line(sb, d, "Estate \"${e.name}\"")
        for (s in e.body) estateStmt(sb, s, d + 1)
    }

    private fun estateStmt(sb: StringBuilder, s: EstateStmt, d: Int) = when (s) {
        is LetStmt -> line(sb, d, "Let ${s.name} = ${expr(s.value)}")
        is TypeStmt -> line(sb, d, "Type ${s.value}")
        is OfferTypeStmt -> line(sb, d, "OfferType ${s.value}")
        is LocationStmt -> line(sb, d, "Location ${expr(s.point)}")
        is PriceStmt -> line(sb, d, "Price ${expr(s.value)}")
        is SizeStmt -> line(sb, d, "Size ${expr(s.value)}")
        is RegionStmt -> line(sb, d, "Region \"${s.value}\"")
        is NeighborhoodStmt -> line(sb, d, "Neighborhood \"${s.value}\"")
        is DescriptionStmt -> line(sb, d, "Description \"${s.value}\"")
        is SourceStmt -> line(sb, d, "Source \"${s.value}\"")
        is SetStmt -> line(sb, d, "Set ${s.key} = ${expr(s.value)}")
        is ParcelDecl -> parcel(sb, s, d)
    }

    private fun parcel(sb: StringBuilder, p: ParcelDecl, d: Int) {
        line(sb, d, "Parcel")
        for (c in p.commands) geo(sb, c, d + 1)
    }

    private fun geo(sb: StringBuilder, c: GeoCommand, d: Int) = when (c) {
        is LineCmd -> line(sb, d, "Line ${expr(c.p1)} -> ${expr(c.p2)}")
        is BoxCmd -> line(sb, d, "Box ${expr(c.p1)} -> ${expr(c.p2)}")
        is BendCmd -> line(sb, d, "Bend ${expr(c.p1)} -> ${expr(c.p2)} angle ${expr(c.angle)}")
    }

    private fun expr(e: Expr): String = when (e) {
        is NumberLit -> num(e.value)
        is StringLit -> "\"${e.value}\""
        is BoolLit -> e.value.toString()
        is NilLit -> "nil"
        is VarRef -> e.name
        is PointLit -> "(${expr(e.x)}, ${expr(e.y)})"
        is Unary -> "(${e.op}${expr(e.operand)})"
        is Binary -> "(${expr(e.left)} ${e.op} ${expr(e.right)})"
        is FstExpr -> "fst(${expr(e.arg)})"
        is SndExpr -> "snd(${expr(e.arg)})"
    }

    private fun num(d: Double): String =
        if (!d.isInfinite() && !d.isNaN() && d == Math.floor(d)) d.toLong().toString() else d.toString()
}
