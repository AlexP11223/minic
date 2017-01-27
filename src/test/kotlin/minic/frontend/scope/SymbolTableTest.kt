package minic.frontend.scope

import minic.frontend.ast.*
import minic.frontend.scope.*
import org.junit.Test
import kotlin.test.assertEquals

class SymbolTableTest {

    private fun symbolsStr(scope: Scope): String {
        return scope.allSymbols()
                .map { it.type.name + " " + it.name }
                .joinToString(", ")
    }

    @Test
    fun canProcessWithSymbols() {
        val ast = Program(listOf(
                VariableDeclaration(StringTypeNode(), "str", StringLiteral("Hello")),
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0))
                        )))
        ))

        val result = mutableListOf<String>()

        ast.processWithSymbols(beforeSymbolOperation = { node, scope ->
            result.add(("BeforeSymbol " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        }, enterOperation = { node, scope ->
            result.add(("Enter " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        }, exitOperation = { node, scope ->
            result.add(("Exit " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        })

        assertEquals(listOf(
                "BeforeSymbol Program",
                "Enter Program",
                "BeforeSymbol VariableDeclaration",
                "Enter VariableDeclaration: string str",
                "BeforeSymbol StringTypeNode: string str",
                "Enter StringTypeNode: string str",
                "Exit StringTypeNode: string str",
                "BeforeSymbol StringLiteral: string str",
                "Enter StringLiteral: string str",
                "Exit StringLiteral: string str",
                "Exit VariableDeclaration: string str",
                "BeforeSymbol WhileStatement: string str",
                "Enter WhileStatement: string str",
                "BeforeSymbol BooleanLiteral: string str",
                "Enter BooleanLiteral: string str",
                "Exit BooleanLiteral: string str",
                "BeforeSymbol StatementsBlock: string str",
                "Enter StatementsBlock: string str",
                "BeforeSymbol VariableDeclaration: string str",
                "Enter VariableDeclaration: string str, int i",
                "BeforeSymbol IntTypeNode: string str, int i",
                "Enter IntTypeNode: string str, int i",
                "Exit IntTypeNode: string str, int i",
                "BeforeSymbol IntLiteral: string str, int i",
                "Enter IntLiteral: string str, int i",
                "Exit IntLiteral: string str, int i",
                "Exit VariableDeclaration: string str, int i",
                "Exit StatementsBlock: string str, int i",
                "Exit WhileStatement: string str",
                "Exit Program: string str"
        ), result)
    }

    @Test
    fun canProcessWithSymbolsSpecificNodeType() {
        val ast = Program(listOf(
                VariableDeclaration(StringTypeNode(), "str", StringLiteral("Hello")),
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0))
                        )))
        ))

        val result = mutableListOf<String>()

        ast.processWithSymbols(VariableDeclaration::class.java, beforeSymbolOperation = { node, scope ->
            result.add(("BeforeSymbol " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        }, enterOperation = { node, scope ->
            result.add(("Enter " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        }, exitOperation = { node, scope ->
            result.add(("Exit " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        })

        assertEquals(listOf(
                "BeforeSymbol VariableDeclaration",
                "Enter VariableDeclaration: string str",
                "Exit VariableDeclaration: string str",
                "BeforeSymbol VariableDeclaration: string str",
                "Enter VariableDeclaration: string str, int i",
                "Exit VariableDeclaration: string str, int i"
        ), result)
    }

    @Test
    fun canProcessWithSymbolsUntil() {
        val ast = Program(listOf(
                VariableDeclaration(StringTypeNode(), "str", StringLiteral("Hello")),
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0))
                        )))
        ))

        val result = mutableListOf<String>()

        ast.processWithSymbolsUntil(beforeSymbolOperation = { node, scope ->
            result.add(("BeforeSymbol " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        }, enterOperation = fun(node, scope): Boolean {
            result.add(("Enter " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
            return node !is WhileStatement
        }, exitOperation = { node, scope ->
            result.add(("Exit " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        })

        assertEquals(listOf(
                "BeforeSymbol Program",
                "Enter Program",
                "BeforeSymbol VariableDeclaration",
                "Enter VariableDeclaration: string str",
                "BeforeSymbol StringTypeNode: string str",
                "Enter StringTypeNode: string str",
                "Exit StringTypeNode: string str",
                "BeforeSymbol StringLiteral: string str",
                "Enter StringLiteral: string str",
                "Exit StringLiteral: string str",
                "Exit VariableDeclaration: string str",
                "BeforeSymbol WhileStatement: string str",
                "Enter WhileStatement: string str",
                "Exit Program: string str"
        ), result)
    }

    @Test
    fun canProcessSpecificNodeTypeWithSymbolsUntil() {
        val ast = Program(listOf(
                VariableDeclaration(StringTypeNode(), "str", StringLiteral("Hello")),
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntTypeNode(), "i", IntLiteral(0))
                        )))
        ))

        val result = mutableListOf<String>()

        ast.processWithSymbolsUntil(Statement::class.java, beforeSymbolOperation = { node, scope ->
            result.add(("BeforeSymbol " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        }, enterOperation = fun(node, scope): Boolean {
            result.add(("Enter " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
            return node !is WhileStatement
        }, exitOperation = { node, scope ->
            result.add(("Exit " + node.javaClass.simpleName + ": " + symbolsStr(scope)).trim(' ', ':'))
        })

        assertEquals(listOf(
                "BeforeSymbol VariableDeclaration",
                "Enter VariableDeclaration: string str",
                "Exit VariableDeclaration: string str",
                "BeforeSymbol WhileStatement: string str",
                "Enter WhileStatement: string str"
        ), result)
    }

}