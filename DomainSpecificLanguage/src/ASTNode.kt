package src

// Osnovni vmesnik za vsa vozlišča v drevesu
sealed interface ASTNode

/**
 * 1. Koren programa in splošna struktura
 * BNF: Program ::= ListingsDecl eof;
 */
data class ProgramNode(val listings: List<ListingsDeclNode>) : ASTNode

// BNF: ListingsDecl ::= listings string lbrace ListingsBody rbrace;
data class ListingsDeclNode(val name: String, val body: List<ListingsStmtNode>) : ASTNode

// BNF: ListingsStmt ::= LetDecl | EstateDecl;
sealed interface ListingsStmtNode : ASTNode

// BNF: EstateDecl ::= estate string lbrace EstateBody rbrace;
data class EstateDeclNode(val name: String, val body: List<EstateStmtNode>) : ListingsStmtNode


/**
 * 2. Stavki znotraj nepremičnin / struktur (Estate Statements)
 * BNF: EstateStmt ::= ...
 */
sealed interface EstateStmtNode : ASTNode

// BNF: type identifier semicolon
data class TypeStmtNode(val value: String) : EstateStmtNode

// BNF: offerType identifier semicolon
data class OfferTypeStmtNode(val value: String) : EstateStmtNode

// BNF: location PointExpr semicolon
data class LocationStmtNode(val pointExpr: PointExprNode) : EstateStmtNode

// BNF: price Expr semicolon
data class PriceStmtNode(val expr: ExprNode) : EstateStmtNode

// BNF: size Expr semicolon
data class SizeStmtNode(val expr: ExprNode) : EstateStmtNode

// BNF: region string semicolon
data class RegionStmtNode(val value: String) : EstateStmtNode

// BNF: neighborhood string semicolon
data class NeighborhoodStmtNode(val value: String) : EstateStmtNode

// BNF: description string semicolon
data class DescriptionStmtNode(val value: String) : EstateStmtNode

// BNF: source string semicolon
data class SourceStmtNode(val value: String) : EstateStmtNode

// BNF: SetStmt ::= set lparen string comma SetValue rparen semicolon;
data class SetStmtNode(val key: String, val value: SetValueNode) : EstateStmtNode

// BNF: ParcelDecl ::= parcel lbrace ParcelBody rbrace;
data class ParcelDeclNode(val commands: List<GeoCommandNode>) : EstateStmtNode

// BNF: LetDecl ::= let identifier assign LetValue semicolon;
// Opomba: LetDecl se lahko pojavi v ListingsStmt ali v EstateStmt
data class LetDeclNode(val identifier: String, val value: LetValueNode) : ListingsStmtNode, EstateStmtNode


/**
 * 3. Vrednosti in Geometrijski ukazi
 */
// BNF: LetValue ::= Point | Expr;
sealed interface LetValueNode : ASTNode

// BNF: SetValue ::= Point | Expr;
sealed interface SetValueNode : ASTNode

// BNF: GeoCommand ::= line(...) | box(...) | bend(...)
sealed interface GeoCommandNode : ASTNode

data class LineCommandNode(val start: PointExprNode, val end: PointExprNode) : GeoCommandNode
data class BoxCommandNode(val minCorner: PointExprNode, val maxCorner: PointExprNode) : GeoCommandNode
data class BendCommandNode(val start: PointExprNode, val end: PointExprNode, val radius: ExprNode) : GeoCommandNode


/**
 * 4. Točke in Izrazi
 */
// BNF: PointExpr ::= Point | identifier;
sealed interface PointExprNode : ASTNode

// BNF: Point ::= lparen Expr comma Expr rparen;
// Točka implementira več vmesnikov, saj nastopa kot LetValue, SetValue in PointExpr
data class PointNode(
    val x: ExprNode,
    val y: ExprNode
) : PointExprNode, LetValueNode, SetValueNode

// BNF: Expr ::= Additive;
sealed interface ExprNode : ASTNode, LetValueNode, SetValueNode

data class BinaryExprNode(val left: ExprNode, val op: String, val right: ExprNode) : ExprNode
data class UnaryExprNode(val op: String, val expr: ExprNode) : ExprNode

// Za literale: števila, nize, booleane, nil
data class LiteralExprNode(val value: Any?) : ExprNode

// Identifikator lahko nastopa kot samostojen izraz ali kot PointExpr (npr. 'location center_tocka;')
data class IdentifierExprNode(val name: String) : ExprNode, PointExprNode

// Funkcijski klici za dostop do koordinat
data class FstExprNode(val expr: ExprNode) : ExprNode
data class SndExprNode(val expr: ExprNode) : ExprNode