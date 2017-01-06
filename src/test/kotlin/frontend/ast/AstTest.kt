package frontend.ast

import minic.frontend.antlr.MiniCLexer
import minic.frontend.antlr.MiniCParser
import minic.frontend.ast.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Test
import java.io.StringReader
import kotlin.test.assertEquals

class AstTest {
    fun pos(startLine: Int, startCol: Int, endLine: Int, endCol: Int) = Position(Point(startLine,startCol), Point(endLine,endCol))

    fun ast(code: String, setPosition: Boolean = false): Program {
        val lexer = MiniCLexer(ANTLRInputStream(StringReader(code)))
        val parser = MiniCParser(CommonTokenStream(lexer))
        val antlrTree = parser.program()

        val mapper = AntlrToAstMapper(setPosition)
        val ast = mapper.map(antlrTree)
        return ast
    }

    @Test
    fun parsesAssignment() {
        val code = """
x = 42;
xf = 42.5;
xs = "Hello";
xb = true;
xb = false;
y = x;
"""
        val expectedAst = Program(listOf(
                Assignment("x", IntLiteral(42)),
                Assignment("xf", FloatLiteral(42.5)),
                Assignment("xs", StringLiteral("Hello")),
                Assignment("xb", BooleanLiteral(true)),
                Assignment("xb", BooleanLiteral(false)),
                Assignment("y", VariableReference("x"))
        ))

        assertEquals(expectedAst, ast(code))
    }

    @Test
    fun parsesVariableDeclaration() {
        val code =  """
int x = 42;
double xf = 42.5;
string xs = "Hello";
bool xb = true;
int y = x;
"""
        val expectedAst = Program(listOf(
                VariableDeclaration(IntType(), "x", IntLiteral(42)),
                VariableDeclaration(DoubleType(), "xf", FloatLiteral(42.5)),
                VariableDeclaration(StringType(), "xs", StringLiteral("Hello")),
                VariableDeclaration(BooleanType(), "xb", BooleanLiteral(true)),
                VariableDeclaration(IntType(), "y", VariableReference("x"))
        ))

        assertEquals(expectedAst, ast(code))
    }

    @Test
    fun parsesArithmeticExpression() {
        val code =  "x = 1 + 2 * 3/4.0 - (5 + 6 * 7 * (-8 - 9));"
        val expectedAst = Program(listOf(
                Assignment("x",
                        SubtractionExpression(
                                AdditionExpression(
                                        IntLiteral(1),
                                        DivisionExpression(
                                                MultiplicationExpression(
                                                        IntLiteral(2),
                                                        IntLiteral(3)
                                                ),
                                                FloatLiteral(4.0)
                                        )
                                ),
                                AdditionExpression(
                                        IntLiteral(5),
                                        MultiplicationExpression(
                                                MultiplicationExpression(
                                                        IntLiteral(6),
                                                        IntLiteral(7)
                                                ),
                                                SubtractionExpression(
                                                        UnaryMinusExpression(IntLiteral(8)),
                                                        IntLiteral(9)
                                                )
                                        )
                                )
                        )
                )
        ))

        assertEquals(expectedAst, ast(code))
    }

    @Test
    fun parsesModExpression() {
        val code =  "x = 1 + 2 % 3;"
        val expectedAst = Program(listOf(
                Assignment("x",
                        AdditionExpression(
                                IntLiteral(1),
                                ModExpression(
                                        IntLiteral(2),
                                        IntLiteral(3)
                                )
                        )
                )
        ))

        assertEquals(expectedAst, ast(code))
    }

    @Test
    fun parsesLogicalExpression() {
        val code =  "f = a || b && !c;"
        val expectedAst = Program(listOf(
                Assignment("f",
                        OrExpression(
                                VariableReference("a"),
                                AndExpression(
                                        VariableReference("b"),
                                        NotExpression(VariableReference("c"))
                                )
                        )
                )
        ))

        assertEquals(expectedAst, ast(code))
    }

    @Test
    fun parsesWithPosition() {
        val code =
"""x = 42;
int y = 1.5 + x;"""
        val expectedAst = Program(listOf(
                Assignment("x", IntLiteral(42, pos(1, 4, 1, 6)), pos(1, 0, 1, 7)),
                VariableDeclaration(IntType(pos(2, 0, 2, 3)), "y",
                        AdditionExpression(
                                FloatLiteral(1.5, pos(2, 8, 2, 11)),
                                VariableReference("x", pos(2, 14, 2, 15)),
                                pos(2, 8, 2, 15)
                        ), pos(2, 0, 2, 16))
        ), pos(1, 0, 2, 16))

        val ast = ast(code, setPosition = true)
        assertEquals(expectedAst, ast)
    }

}