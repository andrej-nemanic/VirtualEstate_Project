package src.semantics

import src.syntaxAnalyser.*

class Validator {
    private val ev = Evaluator()

    enum class Severity { ERROR, WARNING }

    data class Issue(val severity: Severity, val message: String) {
        override fun toString() = "[$severity] $message"
    }

    fun validate(program: Program): List<Issue> {
        val issues = ArrayList<Issue>()
        val global = Environment()
        val parcels = ArrayList<Pair<String, List<PointVal>>>()

        for (stmt in program.listings.body) {
            when (stmt) {
                is LetStmt -> global.define(stmt.name, ev.eval(stmt.value, global))
                is EstateDecl -> validateEstate(stmt, global, issues, parcels)
            }
        }

        for (i in parcels.indices) {
            for (j in i + 1 until parcels.size) {
                if (Geometry.polygonsOverlap(parcels[i].second, parcels[j].second)) {
                    issues += Issue(
                        Severity.WARNING,
                        "parcels '${parcels[i].first}' and '${parcels[j].first}' overlap"
                    )
                }
            }
        }
        return issues
    }

    private fun validateEstate(
        estate: EstateDecl,
        parent: Environment,
        issues: MutableList<Issue>,
        parcels: MutableList<Pair<String, List<PointVal>>>
    ) {
        val env = Environment(parent)
        for (stmt in estate.body) {
            when (stmt) {
                is LetStmt -> env.define(stmt.name, ev.eval(stmt.value, env))
                is PriceStmt -> {
                    val v = ev.num(ev.eval(stmt.value, env))
                    if (v <= 0) issues += Issue(Severity.ERROR, "estate '${estate.name}': price must be > 0 (got $v)")
                }
                is SizeStmt -> {
                    val v = ev.num(ev.eval(stmt.value, env))
                    if (v <= 0) issues += Issue(Severity.ERROR, "estate '${estate.name}': size must be > 0 (got $v)")
                }
                is ParcelDecl -> {
                    if (!Geometry.isParcelClosed(stmt, env, ev)) {
                        issues += Issue(Severity.WARNING, "estate '${estate.name}': parcel is not closed (last point != first)")
                    }
                    val ring = Geometry.buildClosedRing(stmt, env, ev)
                    if (ring.size >= 4) parcels += estate.name to ring
                }
                else -> { }
            }
        }
    }
}
