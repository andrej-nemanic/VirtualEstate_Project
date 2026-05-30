package src.syntaxAnalyser

object SourcePrinter {

    fun pretty(program: Program): String {
        val sb = StringBuilder()
        val l = program.listings
        sb.append("listings ").append(str(l.name)).append(" {\n")
        for (s in l.body) listingsStmt(sb, s, 1)
        sb.append("}\n")
        return sb.toString()
    }

    fun minify(program: Program): String {
        val b = MinBuilder()
        val l = program.listings
        b.add("listings"); b.add(str(l.name)); b.add("{")
        for (s in l.body) minListings(b, s)
        b.add("}")
        return b.toString()
    }

    private fun pad(d: Int): String = "  ".repeat(d)

    private fun listingsStmt(sb: StringBuilder, s: ListingsStmt, d: Int) {
        when (s) {
            is LetStmt -> sb.append(pad(d)).append("let ").append(s.name).append(" = ")
                .append(expr(s.value, 0, false)).append(";\n")
            is EstateDecl -> {
                sb.append(pad(d)).append("estate ").append(str(s.name)).append(" {\n")
                for (st in s.body) estateStmt(sb, st, d + 1)
                sb.append(pad(d)).append("}\n")
            }
        }
    }

    private fun estateStmt(sb: StringBuilder, s: EstateStmt, d: Int) {
        val p = pad(d)
        when (s) {
            is LetStmt -> sb.append(p).append("let ").append(s.name).append(" = ")
                .append(expr(s.value, 0, false)).append(";\n")
            is TypeStmt -> sb.append(p).append("type ").append(s.value).append(";\n")
            is OfferTypeStmt -> sb.append(p).append("offerType ").append(s.value).append(";\n")
            is LocationStmt -> sb.append(p).append("location ").append(expr(s.point, 0, false)).append(";\n")
            is PriceStmt -> sb.append(p).append("price ").append(expr(s.value, 0, false)).append(";\n")
            is SizeStmt -> sb.append(p).append("size ").append(expr(s.value, 0, false)).append(";\n")
            is RegionStmt -> sb.append(p).append("region ").append(str(s.value)).append(";\n")
            is NeighborhoodStmt -> sb.append(p).append("neighborhood ").append(str(s.value)).append(";\n")
            is DescriptionStmt -> sb.append(p).append("description ").append(str(s.value)).append(";\n")
            is SourceStmt -> sb.append(p).append("source ").append(str(s.value)).append(";\n")
            is SetStmt -> sb.append(p).append("set(").append(str(s.key)).append(", ")
                .append(expr(s.value, 0, false)).append(");\n")
            is ParcelDecl -> {
                sb.append(p).append("parcel {\n")
                for (c in s.commands) sb.append(pad(d + 1)).append(geo(c, false)).append(";\n")
                sb.append(p).append("}\n")
            }
        }
    }

    private fun minListings(b: MinBuilder, s: ListingsStmt) {
        when (s) {
            is LetStmt -> { b.add("let"); b.add(s.name); b.add("="); b.add(expr(s.value, 0, true)); b.add(";") }
            is EstateDecl -> {
                b.add("estate"); b.add(str(s.name)); b.add("{")
                for (st in s.body) minEstate(b, st)
                b.add("}")
            }
        }
    }

    private fun minEstate(b: MinBuilder, s: EstateStmt) {
        when (s) {
            is LetStmt -> { b.add("let"); b.add(s.name); b.add("="); b.add(expr(s.value, 0, true)); b.add(";") }
            is TypeStmt -> { b.add("type"); b.add(s.value); b.add(";") }
            is OfferTypeStmt -> { b.add("offerType"); b.add(s.value); b.add(";") }
            is LocationStmt -> { b.add("location"); b.add(expr(s.point, 0, true)); b.add(";") }
            is PriceStmt -> { b.add("price"); b.add(expr(s.value, 0, true)); b.add(";") }
            is SizeStmt -> { b.add("size"); b.add(expr(s.value, 0, true)); b.add(";") }
            is RegionStmt -> { b.add("region"); b.add(str(s.value)); b.add(";") }
            is NeighborhoodStmt -> { b.add("neighborhood"); b.add(str(s.value)); b.add(";") }
            is DescriptionStmt -> { b.add("description"); b.add(str(s.value)); b.add(";") }
            is SourceStmt -> { b.add("source"); b.add(str(s.value)); b.add(";") }
            is SetStmt -> {
                b.add("set"); b.add("("); b.add(str(s.key)); b.add(",")
                b.add(expr(s.value, 0, true)); b.add(")"); b.add(";")
            }
            is ParcelDecl -> {
                b.add("parcel"); b.add("{")
                for (c in s.commands) { b.add(geo(c, true)); b.add(";") }
                b.add("}")
            }
        }
    }

    private fun geo(c: GeoCommand, compact: Boolean): String {
        val comma = if (compact) "," else ", "
        return when (c) {
            is LineCmd -> "line(" + expr(c.p1, 0, compact) + comma + expr(c.p2, 0, compact) + ")"
            is BoxCmd -> "box(" + expr(c.p1, 0, compact) + comma + expr(c.p2, 0, compact) + ")"
            is BendCmd -> "bend(" + expr(c.p1, 0, compact) + comma + expr(c.p2, 0, compact) +
                comma + expr(c.angle, 0, compact) + ")"
        }
    }

    private fun expr(e: Expr, minPrec: Int, compact: Boolean): String {
        val sp = if (compact) "" else " "
        val comma = if (compact) "," else ", "
        return when (e) {
            is NumberLit -> num(e.value)
            is StringLit -> str(e.value)
            is BoolLit -> e.value.toString()
            is NilLit -> "nil"
            is VarRef -> e.name
            is FstExpr -> "fst(" + expr(e.arg, 0, compact) + ")"
            is SndExpr -> "snd(" + expr(e.arg, 0, compact) + ")"
            is PointLit -> "(" + expr(e.x, 0, compact) + comma + expr(e.y, 0, compact) + ")"
            is Unary -> {
                val s = e.op + expr(e.operand, 3, compact)
                if (3 < minPrec) "($s)" else s
            }
            is Binary -> {
                val pr = if (e.op == "+" || e.op == "-") 1 else 2
                val s = expr(e.left, pr, compact) + sp + e.op + sp + expr(e.right, pr + 1, compact)
                if (pr < minPrec) "($s)" else s
            }
        }
    }

    private fun str(s: String): String = "\"" + s + "\""

    private fun num(d: Double): String =
        if (!d.isInfinite() && !d.isNaN() && d == Math.floor(d)) d.toLong().toString() else d.toString()

    private class MinBuilder {
        private val sb = StringBuilder()
        fun add(s: String) {
            if (s.isEmpty()) return
            if (sb.isNotEmpty() && isWord(sb.last()) && isWord(s.first())) sb.append(' ')
            sb.append(s)
        }
        private fun isWord(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '.'
        override fun toString(): String = sb.toString()
    }
}
