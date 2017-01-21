package minic.frontend.ast

import minic.frontend.type.BoolType
import minic.frontend.type.DoubleType
import minic.frontend.type.IntType
import minic.frontend.type.StringType
import minic.frontend.type.Type


data class Program(val statements: List<Statement>, override val position: Position? = null) : AstNode {
    override fun children(): List<AstNode> = statements
}

//
// Statements
//

interface Statement : AstNode

data class StatementsBlock(val statements: List<Statement>, override val position: Position? = null) : Statement {
    override fun children(): List<AstNode> = statements
}

data class IfStatement(val expr: Expression, val ifBody: Statement, val elseBody: Statement?, override val position: Position? = null) : Statement {
    override fun children(): List<AstNode> = listOf(expr, ifBody, elseBody).filterNotNull()
}

data class WhileStatement(val expr: Expression, val statement: Statement, override val position: Position? = null) : Statement {
    override fun children(): List<AstNode> = listOf(expr, statement)
}

data class BreakStatement(override val position: Position? = null) : Statement

data class VariableDeclaration(val variableType: TypeNode, val variableName: String, val value: Expression, override val position: Position? = null) : Statement {
    override fun children(): List<AstNode> = listOf(variableType, value)
}

data class Assignment(val variableName: String, val value: Expression, override val position: Position? = null) : Statement {
    override fun children(): List<AstNode> = listOf(value)
}

data class ExitStatement(override val position: Position? = null) : Statement

//
// Types
//

interface TypeNode : AstNode {
    val type: Type

    val name: String get() = type.name
}

data class IntTypeNode(override val position: Position? = null) : TypeNode {
    override val type: Type get() = IntType
}

data class DoubleTypeNode(override val position: Position? = null) : TypeNode {
    override val type: Type get() = DoubleType
}

data class StringTypeNode(override val position: Position? = null) : TypeNode {
    override val type: Type get() = StringType
}

data class BoolTypeNode(override val position: Position? = null) : TypeNode {
    override val type: Type get() = BoolType
}

//
// Expressions
//

interface Expression : AstNode

data class IntLiteral(val value: Int, override val position: Position? = null) : Expression
data class FloatLiteral(val value: Double, override val position: Position? = null) : Expression
data class StringLiteral(val value: String, override val position: Position? = null) : Expression
data class BooleanLiteral(val value: Boolean, override val position: Position? = null) : Expression

data class VariableReference(val variableName: String, override val position: Position? = null) : Expression

interface BinaryExpression : Expression {
    val left: Expression
    val right: Expression

    override fun children(): List<AstNode> = listOf(left, right)
}

data class AdditionExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression
data class SubtractionExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression
data class MultiplicationExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression
data class DivisionExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression
data class ModExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression

data class UnaryMinusExpression(val value: Expression, override val position: Position? = null) : Expression {
    override fun children(): List<AstNode> = listOf(value)
}

interface LogicalExpression : Expression

data class NotExpression(val expr: Expression, override val position: Position? = null) : LogicalExpression {
    override fun children(): List<AstNode> = listOf(expr)
}

data class AndExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, LogicalExpression
data class OrExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, LogicalExpression

interface EqualityExpression : Expression
interface RelationalExpression : Expression

data class EqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, EqualityExpression
data class NotEqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, EqualityExpression

data class LessExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, RelationalExpression
data class GreaterExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, RelationalExpression
data class LessOrEqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, RelationalExpression
data class GreaterOrEqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, RelationalExpression

data class ReadInt(override val position: Position? = null) : Expression
data class ReadDouble(override val position: Position? = null) : Expression
data class ReadLine(override val position: Position? = null) : Expression

data class ToString(val value: Expression, override val position: Position? = null) : Expression
