package minic.frontend.ast

import minic.frontend.*
import minic.frontend.scope.Scope
import minic.frontend.type.*

// returns type index
// used in tables to determine expression type
val Type.index: Int
    // probably should not do it like this, should add it to Type interface and override. Left it as it is for better clearness and conciseness
    get() = when(this) {
        is BoolType -> 0
        is StringType -> 1
        is IntType -> 2
        is DoubleType -> 3
        else -> throw UnsupportedOperationException(javaClass.canonicalName)
    }

// Maps type1 - * / % type2 to result type
private val arithmeticResultType: List<List<Type?>> =
        listOf( //                   bool         string       int          double
                /* bool */    listOf(null,        null,        null,        null),
                /* string */  listOf(null,        null,        null,        null),
                /* int */     listOf(null,        null,        IntType,     DoubleType),
                /* double */  listOf(null,        null,        DoubleType,  DoubleType)
                )

// Maps type1 + type2 to result type (not the same as arithmeticResultType, strings can be concatenated)
private val additionResultType: List<List<Type?>> =
        listOf( //                   bool         string       int          double
                /* bool */    listOf(null,        null,        null,        null),
                /* string */  listOf(null,        StringType,  null,        null),
                /* int */     listOf(null,        null,        IntType,     DoubleType),
                /* double */  listOf(null,        null,        DoubleType,  DoubleType)
        )

// Maps type1 == != type2 to result type
private val equalityResultType: List<List<Type?>> =
        listOf( //                   bool         string       int          double
                /* bool */    listOf(BoolType,    null,        null,        null),
                /* string */  listOf(null,        BoolType,    null,        null),
                /* int */     listOf(null,        null,        BoolType,    BoolType),
                /* double */  listOf(null,        null,        BoolType,    BoolType)
        )

// Maps type1 > < >= <= type2 to result type
private val relationalResultType: List<List<Type?>> =
        listOf( //                   bool         string       int          double
                /* bool */    listOf(null,        null,        null,        null),
                /* string */  listOf(null,        null,        null,        null),
                /* int */     listOf(null,        null,        BoolType,    BoolType),
                /* double */  listOf(null,        null,        BoolType,    BoolType)
                )

// Maps type1 && || type2 to result type
private val logicalResultType: List<List<Type?>> =
        listOf( //                   bool         string       int          double
                /* bool */    listOf(BoolType,    null,        null,        null),
                /* string */  listOf(null,        null,        null,        null),
                /* int */     listOf(null,        null,        null,        null),
                /* double */  listOf(null,        null,        null,        null)
        )

private fun resultType(table: List<List<Type?>>, leftType: Type, rightType: Type) : Type? {
    return table[leftType.index][rightType.index]
}

private val promoteFromTo: List<List<Type?>> =
        listOf( //                   bool         string       int          double
                /* bool */    listOf(null,        null,        null,        null),
                /* string */  listOf(null,        null,        null,        null),
                /* int */     listOf(null,        null,        null,        DoubleType),
                /* double */  listOf(null,        null,        null,        null)
        )

// promotes type if needed. Returns the same type if promotion not needed or not possible
fun Type.promoteTo(resultType: Type) : Type {
    return promoteFromTo[this.index][resultType.index] ?: this
}

fun Type.canPromoteTo(resultType: Type) : Boolean {
    return this.promoteTo(resultType) == resultType
}

// returns result type of expression
// throws exception if undefined variable found or incompatible expressions found
fun Expression.type(scope: Scope) : Type {
    return when (this) {
        is IntLiteral -> IntType
        is FloatLiteral -> DoubleType
        is StringLiteral -> StringType
        is BooleanLiteral -> BoolType
        is VariableReference -> scope.resolve(variableName)?.type ?: throw UndefinedSymbolException("Variable '$variableName' is not defined", this)
        is NotExpression -> {
            val exprType = expr.type(scope)
            if (exprType != BoolType) {
                throw IllegalExpressionException("Cannot apply logical NOT to '${exprType.name}', must be '${BoolType.name}'", this)
            }
            return BoolType
        }
        is UnaryMinusExpression -> {
            val valueType = value.type(scope)
            if (valueType != IntType && valueType != DoubleType) {
                throw IllegalExpressionException("Cannot apply unary minus to '${valueType.name}'", this)
            }
            return valueType
        }
        is BinaryExpression -> {
            val leftType = left.type(scope)
            val rightType = right.type(scope)

            return when (this) {
                is AdditionExpression -> resultType(additionResultType, leftType, rightType) ?:
                        throw IllegalExpressionException("Cannot apply this operation to '${leftType.name}' and '${rightType.name}'", this)

                is SubtractionExpression, is MultiplicationExpression, is DivisionExpression, is ModExpression -> resultType(arithmeticResultType, leftType, rightType) ?:
                        throw IllegalExpressionException("Cannot apply this operation to '${leftType.name}' and '${rightType.name}'", this)

                is EqualityExpression -> resultType(equalityResultType, leftType, rightType) ?:
                        throw IllegalExpressionException("Cannot check equality of '${leftType.name}' and '${rightType.name}'", this)

                is RelationalExpression -> resultType(relationalResultType, leftType, rightType) ?:
                        throw IllegalExpressionException("Cannot compare '${leftType.name}' and '${rightType.name}'", this)

                is AndExpression, is OrExpression -> resultType(logicalResultType, leftType, rightType) ?:
                        throw IllegalExpressionException("Cannot apply logical operation to '${leftType.name}' and '${rightType.name}', must be '${BoolType.name}'", this)

                else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
            }
        }
        else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
    }
}
