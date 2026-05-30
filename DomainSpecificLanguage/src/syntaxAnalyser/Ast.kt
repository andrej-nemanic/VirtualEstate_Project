package src.syntaxAnalyser

class Program(val listings: Listings)

class Listings(val name: String, val body: List<ListingsStmt>)

sealed interface ListingsStmt

sealed interface EstateStmt

class LetStmt(val name: String, val value: Expr) : ListingsStmt, EstateStmt
class EstateDecl(val name: String, val body: List<EstateStmt>) : ListingsStmt

class TypeStmt(val value: String) : EstateStmt
class OfferTypeStmt(val value: String) : EstateStmt
class LocationStmt(val point: Expr) : EstateStmt
class PriceStmt(val value: Expr) : EstateStmt
class SizeStmt(val value: Expr) : EstateStmt
class RegionStmt(val value: String) : EstateStmt
class NeighborhoodStmt(val value: String) : EstateStmt
class DescriptionStmt(val value: String) : EstateStmt
class SourceStmt(val value: String) : EstateStmt
class SetStmt(val key: String, val value: Expr) : EstateStmt
class ParcelDecl(val commands: List<GeoCommand>) : EstateStmt

sealed interface GeoCommand
class LineCmd(val p1: Expr, val p2: Expr) : GeoCommand
class BoxCmd(val p1: Expr, val p2: Expr) : GeoCommand
class BendCmd(val p1: Expr, val p2: Expr, val angle: Expr) : GeoCommand

sealed interface Expr
class NumberLit(val value: Double) : Expr
class StringLit(val value: String) : Expr
class BoolLit(val value: Boolean) : Expr
object NilLit : Expr
class VarRef(val name: String) : Expr
class PointLit(val x: Expr, val y: Expr) : Expr
class Unary(val op: String, val operand: Expr) : Expr
class Binary(val op: String, val left: Expr, val right: Expr) : Expr
class FstExpr(val arg: Expr) : Expr
class SndExpr(val arg: Expr) : Expr
