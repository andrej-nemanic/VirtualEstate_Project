package src.geojson

import src.semantics.*
import src.syntaxAnalyser.*
import kotlin.math.abs
import kotlin.math.floor

class GeoJsonGenerator {
    private val ev = Evaluator()

    fun generate(program: Program): String {
        val features = ArrayList<String>()
        val global = Environment()
        for (stmt in program.listings.body) {
            when (stmt) {
                is LetStmt -> global.define(stmt.name, ev.eval(stmt.value, global))
                is EstateDecl -> features += estateFeatures(stmt, global, program.listings.name)
            }
        }
        return featureCollection(features)
    }

    private fun estateFeatures(estate: EstateDecl, parent: Environment, listingsName: String): List<String> {
        val env = Environment(parent)
        val props = LinkedHashMap<String, Any?>()
        props["name"] = estate.name
        props["listings"] = listingsName
        var location: PointVal? = null
        var parcel: ParcelDecl? = null

        for (stmt in estate.body) {
            when (stmt) {
                is LetStmt -> env.define(stmt.name, ev.eval(stmt.value, env))
                is TypeStmt -> props["type"] = stmt.value
                is OfferTypeStmt -> props["offerType"] = stmt.value
                is LocationStmt -> location = ev.point(ev.eval(stmt.point, env))
                is PriceStmt -> props["price"] = ev.num(ev.eval(stmt.value, env))
                is SizeStmt -> props["size"] = ev.num(ev.eval(stmt.value, env))
                is RegionStmt -> props["region"] = stmt.value
                is NeighborhoodStmt -> props["neighborhood"] = stmt.value
                is DescriptionStmt -> props["description"] = stmt.value
                is SourceStmt -> props["source"] = stmt.value
                is SetStmt -> props[stmt.key] = jsonable(ev.eval(stmt.value, env))
                is ParcelDecl -> parcel = stmt
            }
        }

        val features = ArrayList<String>()
        location?.let { features += feature(pointGeometry(it), props) }
        parcel?.let {
            val ring = Geometry.buildClosedRing(it, env, ev)
            if (ring.size >= 4) features += feature(polygonGeometry(ring), props)
        }
        return features
    }

    private fun featureCollection(features: List<String>): String {
        val body = features.joinToString(",\n") { indent(it) }
        return "{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n$body\n  ]\n}"
    }

    private fun feature(geometry: String, props: Map<String, Any?>): String =
        "{ \"type\": \"Feature\", \"geometry\": $geometry, \"properties\": ${propsJson(props)} }"

    private fun pointGeometry(p: PointVal): String =
        "{ \"type\": \"Point\", \"coordinates\": [${num(p.x)}, ${num(p.y)}] }"

    private fun polygonGeometry(ring: List<PointVal>): String {
        val coords = ring.joinToString(", ") { "[${num(it.x)}, ${num(it.y)}]" }
        return "{ \"type\": \"Polygon\", \"coordinates\": [[$coords]] }"
    }

    private fun propsJson(props: Map<String, Any?>): String =
        "{ " + props.entries.joinToString(", ") { (k, v) -> "${jsonStr(k)}: ${jsonVal(v)}" } + " }"

    private fun jsonVal(v: Any?): String = when (v) {
        null -> "null"
        is String -> jsonStr(v)
        is Double -> num(v)
        is Boolean -> v.toString()
        is DoubleArray -> "[" + v.joinToString(", ") { num(it) } + "]"
        else -> jsonStr(v.toString())
    }

    private fun jsonable(v: Value): Any? = when (v) {
        is NumberVal -> v.n
        is StringVal -> v.s
        is BoolVal -> v.b
        is NilVal -> null
        is PointVal -> doubleArrayOf(v.x, v.y)
    }

    private fun num(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return "0"
        if (d == floor(d) && abs(d) < 1e15) return d.toLong().toString()
        return (Math.round(d * 1e9) / 1e9).toString()
    }

    private fun jsonStr(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(c)
        }
        return sb.append("\"").toString()
    }

    private fun indent(s: String): String =
        s.split("\n").joinToString("\n") { "    $it" }
}
