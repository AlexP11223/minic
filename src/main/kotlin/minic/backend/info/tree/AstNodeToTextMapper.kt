package minic.backend.info.tree

import minic.frontend.ast.*

internal fun AstNode.toText() : String = when (this) {
    is Program -> "program"

    is IfStatement -> "if"
    is WhileStatement -> "while"
    is BreakStatement -> "break"
    is PrintStatement -> "print"
    is ExitStatement -> "exit"
    is EmptyStatement -> "<empty>"
    is StatementsBlock -> "{ } block"
    is Assignment -> "$variableName ="
    is VariableDeclaration -> "declare $variableName"

    is TypeNode -> name

    is IntLiteral -> value.toString()
    is FloatLiteral -> value.toString()
    is BooleanLiteral -> value.toString()
    is StringLiteral -> "\"$value\""

    is VariableReference -> variableName

    is AdditionExpression -> "+"
    is SubtractionExpression -> "-"
    is MultiplicationExpression -> "*"
    is DivisionExpression -> "/"
    is ModExpression -> "mod"
    is UnaryMinusExpression -> "unary -"

    is NotExpression -> "!"
    is AndExpression -> "&&"
    is OrExpression -> "||"

    is EqualExpression -> "="
    is NotEqualExpression -> "!="
    is LessExpression -> "<"
    is GreaterExpression -> ">"
    is LessOrEqualExpression -> "<="
    is GreaterOrEqualExpression -> ">="

    is InputFunction -> javaClass.simpleName.decapitalize()
    is ToString -> "toString"

    else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
}
