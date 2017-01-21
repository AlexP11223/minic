package minic.frontend.antlr

import com.tngtech.java.junit.dataprovider.DataProvider
import com.tngtech.java.junit.dataprovider.DataProviderRunner
import com.tngtech.java.junit.dataprovider.UseDataProvider
import minic.frontend.antlr.MiniCLexer
import org.antlr.v4.runtime.ANTLRInputStream
import org.junit.Test
import org.junit.runner.RunWith
import java.io.StringReader
import java.util.*
import kotlin.test.assertEquals

@RunWith(DataProviderRunner::class)
class LexerTest {

    companion object {
        @DataProvider
        @JvmStatic
        fun tokensDataProvider() : Array<Array<Any>> = arrayOf(
                arrayOf(listOf("INT_TYPE", "Identifier", "ASSIGN", "IntegerLiteral", "SEMI", "EOF"),
                        "int myVar = 42;"),
                arrayOf(listOf("DOUBLE_TYPE", "Identifier", "ASSIGN", "FloatingPointLiteral", "SEMI", "EOF"),
                        "double myVar = 42.0;"),
                arrayOf(listOf("STRING_TYPE", "Identifier", "ASSIGN", "StringLiteral", "SEMI", "EOF"),
                        "string myVar = \"Hello\";"),
                arrayOf(listOf("BOOL_TYPE", "Identifier", "ASSIGN", "BooleanLiteral", "SEMI",
                                "Identifier", "ASSIGN", "BooleanLiteral", "SEMI", "EOF"),
                        "bool myVar = true; myVar = false;"),

                arrayOf(listOf("INT_TYPE", "Identifier", "ASSIGN", "READ_INT_KEYWORD", "LPAR", "RPAR", "SEMI", "EOF"),
                        "int myVar = readInt();"),
                arrayOf(listOf("DOUBLE_TYPE", "Identifier", "ASSIGN", "READ_DOUBLE_KEYWORD", "LPAR", "RPAR", "SEMI", "EOF"),
                        "double myVar = readDouble();"),
                arrayOf(listOf("STRING_TYPE", "Identifier", "ASSIGN", "READ_LINE_KEYWORD", "LPAR", "RPAR", "SEMI", "EOF"),
                        "string myVar = readLine();"),

                arrayOf(listOf("Identifier", "MUL", "IntegerLiteral", "PLUS",
                                "Identifier", "DIV", "MINUS", "IntegerLiteral", "MINUS", "Identifier", "MINUS",
                                "LPAR", "Identifier", "MOD", "IntegerLiteral", "RPAR", "EOF"),
                        "a * 2 + b / -3 - c - (c % 2)"),
                arrayOf(listOf("NOT", "Identifier", "EOF"),
                        "!flag"),
                arrayOf(listOf("LPAR", "Identifier", "EQ", "IntegerLiteral", "AND", "Identifier", "GT", "Identifier", "RPAR", "OR",
                                "LPAR", "Identifier", "NOTEQ", "IntegerLiteral", "AND", "Identifier", "LT", "Identifier", "AND",
                                "Identifier", "LTEQ", "IntegerLiteral", "AND", "Identifier", "GTEQ", "MINUS", "IntegerLiteral", "RPAR", "EOF"),
                        "(a == 42 && a > b) || (a != 43 && b < c && c <= 0 && d >= -1)"),
                arrayOf(listOf("IF_KEYWORD", "LPAR", "Identifier", "GT", "IntegerLiteral", "RPAR",
                        "LBRACE", "EXIT_KEYWORD", "LPAR", "RPAR", "SEMI", "RBRACE", "EOF"),
                        """
if (x > 10) {
    exit();
}
                        """),
                arrayOf(listOf("WHILE_KEYWORD", "LPAR", "BooleanLiteral", "RPAR", "LBRACE", "RBRACE", "EOF"),
                        "while (true) {}"),
                arrayOf(listOf("WHILE_KEYWORD", "LPAR", "BooleanLiteral", "RPAR",
                        "LBRACE",
                        "IF_KEYWORD", "LPAR", "Identifier", "GT", "IntegerLiteral", "RPAR",
                        "LBRACE", "BREAK_KEYWORD", "SEMI", "RBRACE",
                        "ELSE_KEYWORD",
                        "LBRACE", "CONTINUE_KEYWORD", "SEMI", "RBRACE",
                        "RBRACE", "EOF"),
                        """
while (true) {
    if (c > 0) {
        break;
    } else {
        continue;
    }
}
                        """)
        )
    }

    fun tokens(code: String) : List<String> {
        val lexer = MiniCLexer(ANTLRInputStream(StringReader(code)))
        val tokens = ArrayList<String>()
        do {
            val t = lexer.nextToken()
            when (t.type) {
                -1 -> tokens.add("EOF")
                else -> tokens.add(MiniCLexer.VOCABULARY.getSymbolicName(t.type))
            }
        } while (t.type != -1)
        return tokens
    }

    @Test
    @UseDataProvider("tokensDataProvider")
    fun producesCorrectTokens(expectedTokens: List<String>, code: String) {
        assertEquals(expectedTokens, tokens(code))
    }
}
