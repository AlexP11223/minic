package minic.frontend.ast


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

data class VariableDeclaration(val variableType: Type, val variableName: String, val value: Expression, override val position: Position? = null) : Statement {
    override fun children(): List<AstNode> = listOf(variableType, value)
}

data class Assignment(val variableName: String, val value: Expression, override val position: Position? = null) : Statement {
    override fun children(): List<AstNode> = listOf(value)
}

//
// Types
//

interface Type : AstNode {
    val name: String
        get() = javaClass.simpleName.removeSuffix("Type").toLowerCase()
}

data class IntType(override val position: Position? = null) : Type
data class DoubleType(override val position: Position? = null) : Type
data class StringType(override val position: Position? = null) : Type
data class BoolType(override val position: Position? = null) : Type

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

interface ComparisonExpression : Expression

data class EqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, ComparisonExpression
data class NotEqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, ComparisonExpression
data class LessExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, ComparisonExpression
data class GreaterExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, ComparisonExpression
data class LessOrEqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, ComparisonExpression
data class GreaterOrEqualExpression(override val left: Expression, override val right: Expression, override val position: Position? = null) : BinaryExpression, ComparisonExpression
