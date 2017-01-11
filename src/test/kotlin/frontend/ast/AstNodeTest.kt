package frontend.ast

import minic.frontend.ast.*
import org.junit.Test
import kotlin.test.assertEquals

class AstNodeTest {

    @Test
    fun canProcess() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntType(), "i", IntLiteral(0)),
                                BreakStatement()
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        Assignment("x", SubtractionExpression(VariableReference("x"), IntLiteral(42))))
        ))

        val result = mutableListOf<String>()

        ast.process {
            result.add(it.javaClass.simpleName)
        }

        assertEquals(listOf(
                "Program",
                "WhileStatement",
                "BooleanLiteral",
                "StatementsBlock",
                "VariableDeclaration",
                "IntType",
                "IntLiteral",
                "BreakStatement",
                "IfStatement",
                "NotExpression",
                "VariableReference",
                "Assignment",
                "AdditionExpression",
                "VariableReference",
                "IntLiteral",
                "Assignment",
                "SubtractionExpression",
                "VariableReference",
                "IntLiteral"
        ), result)
    }

    @Test
    fun canProcessSpecificNodeType() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntType(), "i", IntLiteral(0)),
                                BreakStatement()
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        Assignment("x", SubtractionExpression(VariableReference("x"), IntLiteral(42)))),
                VariableDeclaration(StringType(), "s", StringLiteral("Hello"))
        ))

        val result = mutableListOf<String>()

        ast.process(VariableDeclaration::class.java) {
            result.add(it.javaClass.simpleName + " " + it.variableName)
        }

        assertEquals(listOf(
                "VariableDeclaration i",
                "VariableDeclaration s"
        ), result)
    }

    @Test
    fun canProcessSpecificNodeBaseType() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntType(), "i", IntLiteral(0)),
                                BreakStatement()
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        null)
        ))

        val result = mutableListOf<String>()

        ast.process(Expression::class.java) {
            result.add(it.javaClass.simpleName)
        }

        assertEquals(listOf(
                "BooleanLiteral",
                "IntLiteral",
                "NotExpression",
                "VariableReference",
                "AdditionExpression",
                "VariableReference",
                "IntLiteral"
        ), result)
    }

    @Test
    fun canProcessUntil() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntType(), "i", IntLiteral(0)),
                                BreakStatement()
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        null)
        ))

        val result = mutableListOf<String>()

        ast.processUntil(fun(it: AstNode): Boolean {
            result.add(it.javaClass.simpleName)
            return !(it is WhileStatement)
        })

        assertEquals(listOf(
                "Program",
                "WhileStatement",
                "IfStatement",
                "NotExpression",
                "VariableReference",
                "Assignment",
                "AdditionExpression",
                "VariableReference",
                "IntLiteral"
        ), result)
    }
}
