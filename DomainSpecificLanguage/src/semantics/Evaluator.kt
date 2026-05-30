package src.semantics

import src.syntaxAnalyser.*

sealed interface Value
class NumberVal(val n: Double) : Value { override fun toString() = "number $n" }
class StringVal(val s: String) : Value { override fun toString() = "string \"$s\"" }
class BoolVal(val b: Boolean) : Value { override fun toString() = b.toString() }
object NilVal : Value { override fun toString() = "nil" }
class PointVal(val x: Double, val y: Double) : Value { override fun toString() = "($x, $y)" }

class EvalException(message: String) : RuntimeException(message)

class Environment(private val parent: Environment? = null) {
    private val vars = HashMap<String, Value>()
    fun define(name: String, v: Value) { vars[name] = v }
    fun get(name: String): Value =
        vars[name] ?: parent?.get(name)
        ?: throw EvalException("Semantic error: undefined variable '$name'")
}

class Evaluator {
    fun eval(e: Expr, env: Environment): Value = when (e) {
        is NumberLit -> NumberVal(e.value)
        is StringLit -> StringVal(e.value)
        is BoolLit -> BoolVal(e.value)
        is NilLit -> NilVal
        is VarRef -> env.get(e.name)
        is PointLit -> PointVal(num(eval(e.x, env)), num(eval(e.y, env)))
        is Unary -> {
            val v = num(eval(e.operand, env))
            NumberVal(if (e.op == "-") -v else v)
        }
        is Binary -> {
            val l = num(eval(e.left, env))
            val r = num(eval(e.right, env))
            NumberVal(
                when (e.op) {
                    "+" -> l + r
                    "-" -> l - r
                    "*" -> l * r
                    "/" -> {
                        if (r == 0.0) throw EvalException("Semantic error: division by zero")
                        l / r
                    }
                    else -> throw EvalException("Semantic error: unknown operator '${e.op}'")
                }
            )
        }
        is FstExpr -> NumberVal(point(eval(e.arg, env)).x)
        is SndExpr -> NumberVal(point(eval(e.arg, env)).y)
    }

    fun num(v: Value): Double = (v as? NumberVal)?.n
        ?: throw EvalException("Semantic error: expected number, got $v")

    fun point(v: Value): PointVal = (v as? PointVal)
        ?: throw EvalException("Semantic error: expected point, got $v")
}
