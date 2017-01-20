package minic.frontend.ast

import minic.frontend.ast.*
import org.junit.Test
import kotlin.test.assertEquals

class AstNodeTest {

    @Test
    fun canProcess() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", UnaryMinusExpression(IntLiteral(1))),
                                BreakStatement()
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        Assignment("x", SubtractionExpression(VariableReference("x"), IntLiteral(42)))),
                ExitStatement()
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
                "IntTypeNode",
                "UnaryMinusExpression",
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
                "IntLiteral",
                "ExitStatement"
        ), result)
    }

    @Test
    fun canProcessSpecificNodeType() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0)),
                                BreakStatement()
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        Assignment("x", SubtractionExpression(VariableReference("x"), IntLiteral(42)))),
                VariableDeclaration(StringTypeNode(), "s", StringLiteral("Hello"))
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
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0)),
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
    fun canProcessSpecificNodeMultipleTypes() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0)),
                                BreakStatement()
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        Assignment("x", SubtractionExpression(VariableReference("x"), IntLiteral(42)))),
                VariableDeclaration(StringTypeNode(), "s", StringLiteral("Hello"))
        ))

        val result = mutableListOf<String>()

        ast.process(listOf(VariableDeclaration::class.java, BinaryExpression::class.java)) {
            result.add(it.javaClass.simpleName)
        }

        assertEquals(listOf(
                "VariableDeclaration",
                "AdditionExpression",
                "SubtractionExpression",
                "VariableDeclaration"
        ), result)
    }

    @Test
    fun canProcessUntil() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0)),
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

    @Test
    fun canProcessEnterExit() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0)),
                                BreakStatement()
                        )))
        ))

        val result = mutableListOf<String>()

        ast.process(enterOperation = {
            result.add("Enter " + it.javaClass.simpleName)
        }, exitOperation = {
            result.add("Exit " + it.javaClass.simpleName)
        })

        assertEquals(listOf(
                "Enter Program",
                "Enter WhileStatement",
                "Enter BooleanLiteral",
                "Exit BooleanLiteral",
                "Enter StatementsBlock",
                "Enter VariableDeclaration",
                "Enter IntTypeNode",
                "Exit IntTypeNode",
                "Enter IntLiteral",
                "Exit IntLiteral",
                "Exit VariableDeclaration",
                "Enter BreakStatement",
                "Exit BreakStatement",
                "Exit StatementsBlock",
                "Exit WhileStatement",
                "Exit Program"
        ), result)
    }

    @Test
    fun canProcessEnterExitSpecificNodeType() {
        val ast = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0)),
                                BreakStatement(),
                                IfStatement(NotExpression(VariableReference("flag")),
                                        StatementsBlock(listOf()),
                                        StatementsBlock(listOf(
                                                Assignment("x", SubtractionExpression(VariableReference("x"), IntLiteral(42))))
                                        ))
                        )))
        ))

        val result = mutableListOf<String>()

        ast.process(StatementsBlock::class.java, enterOperation = {
            result.add("Enter " + it.javaClass.simpleName + " ${it.statements.count()} statements")
        }, exitOperation = {
            result.add("Exit " + it.javaClass.simpleName + " ${it.statements.count()} statements")
        })

        assertEquals(listOf(
                "Enter StatementsBlock 3 statements",
                "Enter StatementsBlock 0 statements",
                "Exit StatementsBlock 0 statements",
                "Enter StatementsBlock 1 statements",
                "Exit StatementsBlock 1 statements",
                "Exit StatementsBlock 3 statements"
        ), result)
    }
}
