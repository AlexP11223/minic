package frontend.antlr

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
                arrayOf(listOf("INT_TYPE", "IDENTIFIER", "ASSIGN", "IntegerLiteral", "SEMI", "EOF"),
                        "int myVar = 42;"),
                arrayOf(listOf("DOUBLE_TYPE", "IDENTIFIER", "ASSIGN", "FloatingPointLiteral", "SEMI", "EOF"),
                        "double myVar = 42.0;"),
                arrayOf(listOf("STRING_TYPE", "IDENTIFIER", "ASSIGN", "StringLiteral", "SEMI", "EOF"),
                        "string myVar = \"Hello\";"),
                arrayOf(listOf("BOOL_TYPE", "IDENTIFIER", "ASSIGN", "BooleanLiteral", "SEMI",
                                "IDENTIFIER", "ASSIGN", "BooleanLiteral", "SEMI", "EOF"),
                        "bool myVar = true; myVar = false;"),
                arrayOf(listOf("IDENTIFIER", "MUL", "IntegerLiteral", "PLUS",
                                "IDENTIFIER", "DIV", "MINUS", "IntegerLiteral", "MINUS", "IDENTIFIER", "MINUS",
                                "LPAR", "IDENTIFIER", "MOD", "IntegerLiteral", "RPAR", "EOF"),
                        "a * 2 + b / -3 - c - (c % 2)"),
                arrayOf(listOf("NOT", "IDENTIFIER", "EOF"),
                        "!flag"),
                arrayOf(listOf("LPAR", "IDENTIFIER", "EQ", "IntegerLiteral", "AND", "IDENTIFIER", "GT", "IDENTIFIER", "RPAR", "OR",
                                "LPAR", "IDENTIFIER", "NOTEQ", "IntegerLiteral", "AND", "IDENTIFIER", "LT", "IDENTIFIER", "AND",
                                "IDENTIFIER", "LTEQ", "IntegerLiteral", "AND", "IDENTIFIER", "GTEQ", "MINUS", "IntegerLiteral", "RPAR", "EOF"),
                        "(a == 42 && a > b) || (a != 43 && b < c && c <= 0 && d >= -1)"),
                arrayOf(listOf("IF_KEYWORD", "LPAR", "IDENTIFIER", "GT", "IntegerLiteral", "RPAR",
                        "LBRACE", "EXIT_KEYWORD", "SEMI", "RBRACE", "EOF"),
                        """
if (x > 10) {
    exit;
}
                        """),
                arrayOf(listOf("WHILE_KEYWORD", "LPAR", "BooleanLiteral", "RPAR", "LBRACE", "RBRACE", "EOF"),
                        "while (true) {}"),
                arrayOf(listOf("WHILE_KEYWORD", "LPAR", "BooleanLiteral", "RPAR",
                        "LBRACE",
                        "IF_KEYWORD", "LPAR", "IDENTIFIER", "GT", "IntegerLiteral", "RPAR",
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
