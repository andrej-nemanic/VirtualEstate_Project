package src.semantics

import src.syntaxAnalyser.*
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

object Geometry {

    const val EPS = 1e-9

    fun samePoint(a: PointVal, b: PointVal): Boolean =
        abs(a.x - b.x) < EPS && abs(a.y - b.y) < EPS

    fun commandPoints(cmd: GeoCommand, env: Environment, ev: Evaluator): List<PointVal> = when (cmd) {
        is LineCmd -> listOf(ev.point(ev.eval(cmd.p1, env)), ev.point(ev.eval(cmd.p2, env)))
        is BoxCmd -> {
            val tl = ev.point(ev.eval(cmd.p1, env))
            val br = ev.point(ev.eval(cmd.p2, env))
            listOf(tl, PointVal(br.x, tl.y), br, PointVal(tl.x, br.y))
        }
        is BendCmd -> bendPoints(
            ev.point(ev.eval(cmd.p1, env)),
            ev.point(ev.eval(cmd.p2, env)),
            ev.num(ev.eval(cmd.angle, env))
        )
    }

    fun buildClosedRing(parcel: ParcelDecl, env: Environment, ev: Evaluator): List<PointVal> {
        val ring = ArrayList<PointVal>()
        for (cmd in parcel.commands) {
            val seq = commandPoints(cmd, env, ev)
            when {
                ring.isEmpty() -> ring += seq
                samePoint(ring.last(), seq.first()) -> ring += seq.drop(1)
                else -> ring += seq
            }
        }
        if (ring.isNotEmpty() && !samePoint(ring.first(), ring.last())) ring += ring.first()
        return ring
    }

    fun isParcelClosed(parcel: ParcelDecl, env: Environment, ev: Evaluator): Boolean {
        val cmds = parcel.commands
        if (cmds.isEmpty()) return true
        if (cmds.size == 1 && cmds[0] is BoxCmd) return true
        val first = commandPoints(cmds.first(), env, ev).first()
        val last = commandPoints(cmds.last(), env, ev).last()
        return samePoint(first, last)
    }

    fun bendPoints(p1: PointVal, p2: PointVal, angleDeg: Double, segments: Int = 16): List<PointVal> {
        if (abs(angleDeg) < EPS) return listOf(p1, p2)
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val chord = hypot(dx, dy)
        if (chord < 1e-12) return listOf(p1, p2)

        val sagitta = (chord / 2.0) * tan(Math.toRadians(angleDeg) / 4.0)
        val mx = (p1.x + p2.x) / 2.0
        val my = (p1.y + p2.y) / 2.0
        val ux = -dy / chord
        val uy = dx / chord
        val cx = mx + ux * (2.0 * sagitta)
        val cy = my + uy * (2.0 * sagitta)

        val pts = ArrayList<PointVal>(segments + 1)
        for (i in 0..segments) {
            val t = i.toDouble() / segments
            val mt = 1.0 - t
            val x = mt * mt * p1.x + 2 * mt * t * cx + t * t * p2.x
            val y = mt * mt * p1.y + 2 * mt * t * cy + t * t * p2.y
            pts += PointVal(x, y)
        }
        return pts
    }

    fun polygonsOverlap(a: List<PointVal>, b: List<PointVal>): Boolean {
        for (i in 0 until a.size - 1) {
            for (j in 0 until b.size - 1) {
                if (segmentsIntersect(a[i], a[i + 1], b[j], b[j + 1])) return true
            }
        }
        if (a.isNotEmpty() && pointInPolygon(a[0], b)) return true
        if (b.isNotEmpty() && pointInPolygon(b[0], a)) return true
        return false
    }

    private fun segmentsIntersect(p1: PointVal, p2: PointVal, p3: PointVal, p4: PointVal): Boolean {
        val d1 = orient(p3, p4, p1)
        val d2 = orient(p3, p4, p2)
        val d3 = orient(p1, p2, p3)
        val d4 = orient(p1, p2, p4)
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
        ) return true
        if (d1 == 0.0 && onSegment(p3, p4, p1)) return true
        if (d2 == 0.0 && onSegment(p3, p4, p2)) return true
        if (d3 == 0.0 && onSegment(p1, p2, p3)) return true
        if (d4 == 0.0 && onSegment(p1, p2, p4)) return true
        return false
    }

    private fun orient(a: PointVal, b: PointVal, c: PointVal): Double =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    private fun onSegment(a: PointVal, b: PointVal, p: PointVal): Boolean =
        p.x >= min(a.x, b.x) - EPS && p.x <= max(a.x, b.x) + EPS &&
            p.y >= min(a.y, b.y) - EPS && p.y <= max(a.y, b.y) + EPS

    private fun pointInPolygon(p: PointVal, ring: List<PointVal>): Boolean {
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val xi = ring[i].x; val yi = ring[i].y
            val xj = ring[j].x; val yj = ring[j].y
            val intersect = (yi > p.y) != (yj > p.y) &&
                p.x < (xj - xi) * (p.y - yi) / (yj - yi) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
