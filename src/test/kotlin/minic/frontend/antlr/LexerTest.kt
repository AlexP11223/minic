package minic.frontend.antlr

import com.tngtech.java.junit.dataprovider.DataProvider
import com.tngtech.java.junit.dataprovider.DataProviderRunner
import com.tngtech.java.junit.dataprovider.UseDataProvider
import minic.Compiler
import minic.frontend.lexer.Token
import org.junit.Test
import org.junit.runner.RunWith
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

                arrayOf(listOf("STRING_TYPE", "Identifier", "ASSIGN", "TO_STRING_KEYWORD", "LPAR", "Identifier", "RPAR", "SEMI", "EOF"),
                        "string myVar = toString(n);"),

                arrayOf(listOf("PRINT_KEYWORD", "LPAR", "StringLiteral", "RPAR", "SEMI", "PRINTLN_KEYWORD", "LPAR", "Identifier", "RPAR", "SEMI", "EOF"),
                        "print(\"Hello \"); println(name);"),

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
        return Compiler(code).tokens.map { it.name }
    }

    @Test
    @UseDataProvider("tokensDataProvider")
    fun producesCorrectTokens(expectedTokens: List<String>, code: String) {
        assertEquals(expectedTokens, tokens(code))
    }

    @Test
    fun skipsLineComments() {
        val code = """
// hello
int myVar = 42; // comment
//int myVar = 43;
/// comment // comment
string myVar = "Hello comment //";
""".trim()
        val tokens = tokens(code);
        assertEquals(listOf(
                "INT_TYPE",
                "Identifier",
                "ASSIGN",
                "IntegerLiteral",
                "SEMI",
                "STRING_TYPE",
                "Identifier",
                "ASSIGN",
                "StringLiteral",
                "SEMI",
                "EOF"), tokens)
    }

    @Test
    fun skipsBlockComments() {
        val code = """
/* hello */
/** hello */
int myVar = 42; /* comment
comment
*/
/*int myVar = 43;/*
/* /* comment // /* comment */
string myVar = "Hello comment /*";
""".trim()
        val tokens = tokens(code);
        assertEquals(listOf(
                "INT_TYPE",
                "Identifier",
                "ASSIGN",
                "IntegerLiteral",
                "SEMI",
                "STRING_TYPE",
                "Identifier",
                "ASSIGN",
                "StringLiteral",
                "SEMI",
                "EOF"), tokens)
    }

    @Test
    fun producesCorrectTokensProperties() {
        val code = """
int myVar = 42;
print(toString(myVar));
""".trim()
        val tokens = Compiler(code).tokens;
        assertEquals(listOf(
                Token(0, 2, 1, "int", "INT_TYPE"),
                Token(4, 8, 1, "myVar", "Identifier"),
                Token(10, 10, 1, "=", "ASSIGN"),
                Token(12, 13, 1, "42", "IntegerLiteral"),
                Token(14, 14, 1, ";", "SEMI"),
                Token(16, 20, 2, "print", "PRINT_KEYWORD"),
                Token(21, 21, 2, "(", "LPAR"),
                Token(22, 29, 2, "toString", "TO_STRING_KEYWORD"),
                Token(30, 30, 2, "(", "LPAR"),
                Token(31, 35, 2, "myVar", "Identifier"),
                Token(36, 36, 2, ")", "RPAR"),
                Token(37, 37, 2, ")", "RPAR"),
                Token(38, 38, 2, ";", "SEMI"),
                Token(39, 38, 2, "<EOF>", "EOF")
        ), tokens)

        assertEquals(3, tokens[0].length)
        assertEquals(5, tokens[1].length)
        assertEquals(1, tokens[2].length)
        assertEquals(0, tokens.last().length)
    }
}
