package minic.frontend.ast

import minic.frontend.IllegalExpressionException
import minic.frontend.UndefinedSymbolException
import minic.frontend.scope.GlobalScope
import minic.frontend.scope.VariableSymbol
import minic.frontend.type.*
import org.junit.Test
import kotlin.test.*

class ExpressionTest {
    private val allTypes = listOf(BoolType, StringType, IntType, DoubleType)

    private val scope = GlobalScope().apply {
        define(VariableSymbol("i", IntType))
        define(VariableSymbol("fnum", DoubleType))
        define(VariableSymbol("str", StringType))
        define(VariableSymbol("flag", BoolType))
    }

    private val typeToVarRefMap = mapOf(
            IntType to VariableReference("i"),
            DoubleType to VariableReference("fnum"),
            StringType to VariableReference("str"),
            BoolType to VariableReference("flag")
    )

    @Test
    fun promotesOnlyIntToDouble() {
        assertTrue(IntType.canPromoteTo(DoubleType))
        assertEquals(DoubleType, IntType.promoteTo(DoubleType))

        for (t1 in allTypes) {
            for (t2 in allTypes) {
                if (t1 is IntType && t2 is DoubleType)
                    continue
                if (t1 == t2) {
                    assertTrue(t1.canPromoteTo(t2), "${t1.name} ${t2.name}")
                } else {
                    assertFalse(t1.canPromoteTo(t2), "${t1.name} ${t2.name}")
                }
                assertEquals(t1, t1.promoteTo(t2), "${t1.name} ${t2.name}")
            }
        }
    }

    @Test
    fun determinesVarRefType() {
        listOf<Pair<Expression, Type>>(
                VariableReference("i") to IntType,
                VariableReference("fnum") to DoubleType,
                VariableReference("str") to StringType,
                VariableReference("flag") to BoolType
        ).forEach {
            val (expr, expectedType) = it
            assertEquals(expectedType, expr.type(scope), "$expr $expectedType")
        }
    }

    @Test
    fun determinesLiteralType() {
        listOf<Pair<Expression, Type>>(
                IntLiteral(42) to IntType,
                FloatLiteral(42.5) to DoubleType,
                StringLiteral("Hello") to StringType,
                BooleanLiteral(true) to BoolType
        ).forEach {
            val (expr, expectedType) = it
            assertEquals(expectedType, expr.type(scope), "$expr $expectedType")
        }
    }

    @Test
    fun determinesUnaryMinusType() {
        listOf<Pair<Expression, Type>>(
                UnaryMinusExpression(IntLiteral(42)) to IntType,
                UnaryMinusExpression(FloatLiteral(42.5)) to DoubleType
        ).forEach {
            val (expr, expectedType) = it
            assertEquals(expectedType, expr.type(scope), "$expr $expectedType")
        }
    }

    @Test
    fun determinesArithmeticExprType() {
        listOf<Pair<Expression, Type>>(
                AdditionExpression(IntLiteral(2), IntLiteral(3)) to IntType,
                SubtractionExpression(IntLiteral(2), IntLiteral(3)) to IntType,
                MultiplicationExpression(IntLiteral(2), IntLiteral(3)) to IntType,
                DivisionExpression(IntLiteral(2), IntLiteral(3)) to IntType,
                ModExpression(IntLiteral(2), IntLiteral(3)) to IntType,

                AdditionExpression(FloatLiteral(2.5), IntLiteral(3)) to DoubleType,
                AdditionExpression(IntLiteral(2), FloatLiteral(2.0)) to DoubleType,
                AdditionExpression(FloatLiteral(2.5), FloatLiteral(2.0)) to DoubleType,
                SubtractionExpression(FloatLiteral(2.5), IntLiteral(3)) to DoubleType,
                MultiplicationExpression(FloatLiteral(2.5), IntLiteral(3)) to DoubleType,
                DivisionExpression(FloatLiteral(2.5), IntLiteral(3)) to DoubleType,
                ModExpression(FloatLiteral(2.5), IntLiteral(3)) to DoubleType,

                AdditionExpression(UnaryMinusExpression(IntLiteral(2)), IntLiteral(3)) to IntType,
                AdditionExpression(FloatLiteral(2.5), UnaryMinusExpression(IntLiteral(3))) to DoubleType,

                AdditionExpression(SubtractionExpression(IntLiteral(42), IntLiteral(8)), MultiplicationExpression(IntLiteral(3), IntLiteral(8))) to IntType,
                AdditionExpression(SubtractionExpression(IntLiteral(42), FloatLiteral(8.0)), MultiplicationExpression(IntLiteral(3), IntLiteral(8))) to DoubleType,

                AdditionExpression(SubtractionExpression(IntLiteral(42), VariableReference("i")), MultiplicationExpression(IntLiteral(3), IntLiteral(8))) to IntType,
                AdditionExpression(SubtractionExpression(IntLiteral(42), VariableReference("fnum")), MultiplicationExpression(IntLiteral(3), IntLiteral(8))) to DoubleType
        ).forEach {
            val (expr, expectedType) = it
            assertEquals(expectedType, expr.type(scope), "$expr $expectedType")
        }
    }

    @Test
    fun determinesStringConcatType() {
        listOf<Pair<Expression, Type>>(
                AdditionExpression(StringLiteral("Hello"), AdditionExpression(StringLiteral(" "), StringLiteral("world"))) to StringType,
                AdditionExpression(StringLiteral("Hello "), VariableReference("str")) to StringType
        ).forEach {
            val (expr, expectedType) = it
            assertEquals(expectedType, expr.type(scope), "$expr $expectedType")
        }
    }

    @Test
    fun determinesEqualityExprType() {
        listOf<Pair<Expression, Expression>>(
                IntLiteral(42) to IntLiteral(42),
                IntLiteral(42) to VariableReference("i"),

                FloatLiteral(42.5) to FloatLiteral(42.5),
                FloatLiteral(42.5) to VariableReference("i"),
                VariableReference("i") to FloatLiteral(42.5),
                FloatLiteral(42.5) to VariableReference("fnum"),

                StringLiteral("Hello") to VariableReference("str"),

                BooleanLiteral(true) to VariableReference("flag"),

                IntLiteral(42) to AdditionExpression(VariableReference("i"), IntLiteral(10)),
                IntLiteral(42) to MultiplicationExpression(VariableReference("i"), IntLiteral(10)),
                StringLiteral("Hello") to AdditionExpression(VariableReference("str"), VariableReference("str")),
                BooleanLiteral(true) to AndExpression(NotExpression(VariableReference("flag")), OrExpression(BooleanLiteral(true), VariableReference("flag")))
        ).forEach {
            val (left, right) = it
            assertEquals(BoolType, EqualExpression(left, right).type(scope), "$left $right")
            assertEquals(BoolType, NotEqualExpression(left, right).type(scope), "$left $right")
        }
    }

    @Test
    fun determinesRelationalExprType() {
        listOf<Pair<Expression, Expression>>(
                IntLiteral(42) to IntLiteral(42),
                IntLiteral(42) to VariableReference("i"),

                FloatLiteral(42.5) to FloatLiteral(42.5),
                FloatLiteral(42.5) to VariableReference("i"),
                VariableReference("i") to FloatLiteral(42.5),
                FloatLiteral(42.5) to VariableReference("fnum"),

                IntLiteral(42) to AdditionExpression(VariableReference("i"), IntLiteral(10)),
                IntLiteral(42) to MultiplicationExpression(VariableReference("i"), IntLiteral(10))
        ).forEach {
            val (left, right) = it
            assertEquals(BoolType, LessExpression(left, right).type(scope), "$left $right")
            assertEquals(BoolType, GreaterExpression(left, right).type(scope), "$left $right")
            assertEquals(BoolType, LessOrEqualExpression(left, right).type(scope), "$left $right")
            assertEquals(BoolType, GreaterOrEqualExpression(left, right).type(scope), "$left $right")
        }
    }

    @Test
    fun determinesNotType() {
        listOf<Pair<Expression, Type>>(
                NotExpression(VariableReference("flag")) to BoolType
        ).forEach {
            val (expr, expectedType) = it
            assertEquals(expectedType, expr.type(scope), "$expr $expectedType")
        }
    }

    @Test
    fun determineLogicalExprType() {
        listOf<Pair<Expression, Expression>>(
                BooleanLiteral(true) to VariableReference("flag"),
                VariableReference("flag") to NotExpression(VariableReference("flag")),
                VariableReference("flag") to AndExpression(NotExpression(VariableReference("flag")), OrExpression(BooleanLiteral(true), VariableReference("flag")))
        ).forEach {
            val (left, right) = it
            assertEquals(BoolType, AndExpression(left, right).type(scope), "$left $right")
            assertEquals(BoolType, OrExpression(left, right).type(scope), "$left $right")
        }
    }

    @Test
    fun failsWhenUndeclaredVar() {
        listOf<Expression>(
                VariableReference("undefined"),
                AdditionExpression(IntLiteral(2), VariableReference("undefined")),
                AndExpression(BooleanLiteral(true), VariableReference("undefinedFlag"))
        ).forEach { expr ->
            assertFailsWith<UndefinedSymbolException>(expr.toString()) {
                expr.type(scope)
            }
        }
    }

    @Test
    fun failsWhenNotWithNotBool() {
        listOf<Expression>(
                NotExpression(IntLiteral(42)),
                NotExpression(FloatLiteral(42.5)),
                NotExpression(StringLiteral("Hello")),
                NotExpression(VariableReference("i"))
        ).forEach { expr ->
            assertFailsWith<IllegalExpressionException>(expr.toString()) {
                expr.type(scope)
            }
        }
    }

    @Test
    fun failsWhenUnaryMinusWithNotNumber() {
        listOf<Expression>(
                UnaryMinusExpression(StringLiteral("Hello")),
                UnaryMinusExpression(BooleanLiteral(true)),
                NotExpression(VariableReference("str"))
        ).forEach { expr ->
            assertFailsWith<IllegalExpressionException>(expr.toString()) {
                expr.type(scope)
            }
        }
    }

    @Test
    fun failsWhenWrongArithmeticExprTypes() {
        val validTypePairs = listOf(
                IntType to IntType,
                IntType to DoubleType,
                DoubleType to IntType,
                DoubleType to DoubleType
        )

        allTypes.forEach { t1 ->
            allTypes.forEach { t2 ->
                if (!validTypePairs.contains(t1 to t2)) {
                    listOf<Expression>(
                            SubtractionExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            MultiplicationExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            DivisionExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            ModExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!)
                    ).forEach { expr ->
                        assertFailsWith<IllegalExpressionException>(expr.toString()) {
                            expr.type(scope)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun failsWhenWrongAddConcatExprTypes() {
        val validTypePairs = listOf(
                IntType to IntType,
                IntType to DoubleType,
                DoubleType to IntType,
                DoubleType to DoubleType,
                StringType to StringType
        )

        allTypes.forEach { t1 ->
            allTypes.forEach { t2 ->
                if (!validTypePairs.contains(t1 to t2)) {
                    listOf<Expression>(
                            AdditionExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!)
                    ).forEach { expr ->
                        assertFailsWith<IllegalExpressionException>(expr.toString()) {
                            expr.type(scope)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun failsWhenWrongEqualityExprTypes() {
        val validTypePairs = listOf(
                BoolType to BoolType,
                StringType to StringType,
                IntType to IntType,
                IntType to DoubleType,
                DoubleType to IntType,
                DoubleType to DoubleType
        )

        allTypes.forEach { t1 ->
            allTypes.forEach { t2 ->
                if (!validTypePairs.contains(t1 to t2)) {
                    listOf<Expression>(
                            EqualExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            NotEqualExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!)
                    ).forEach { expr ->
                        assertFailsWith<IllegalExpressionException>(expr.toString()) {
                            expr.type(scope)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun failsWhenWrongRelationalExprTypes() {
        val validTypePairs = listOf(
                IntType to IntType,
                IntType to DoubleType,
                DoubleType to IntType,
                DoubleType to DoubleType
        )

        allTypes.forEach { t1 ->
            allTypes.forEach { t2 ->
                if (!validTypePairs.contains(t1 to t2)) {
                    listOf<Expression>(
                            LessExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            GreaterExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            LessOrEqualExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            GreaterOrEqualExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!)
                    ).forEach { expr ->
                        assertFailsWith<IllegalExpressionException>(expr.toString()) {
                            expr.type(scope)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun failsWhenWrongLogicalExprTypes() {
        val validTypePairs = listOf(
                BoolType to BoolType
        )

        allTypes.forEach { t1 ->
            allTypes.forEach { t2 ->
                if (!validTypePairs.contains(t1 to t2)) {
                    listOf<Expression>(
                            AndExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!),
                            OrExpression(typeToVarRefMap[t1]!!, typeToVarRefMap[t2]!!)
                    ).forEach { expr ->
                        assertFailsWith<IllegalExpressionException>(expr.toString()) {
                            expr.type(scope)
                        }
                    }
                }
            }
        }
    }
}
