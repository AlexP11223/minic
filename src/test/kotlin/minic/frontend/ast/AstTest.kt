package minic.frontend.ast

import minic.Compiler
import minic.frontend.ast.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AstTest {
    fun pos(startLine: Int, startCol: Int, endLine: Int, endCol: Int) = Position(Point(startLine,startCol), Point(endLine,endCol))

    fun ast(code: String, setPosition: Boolean = false, diagnosticChecks: Boolean = true): Program {
        val parsingResult = Compiler(diagnosticChecks).parse(code)

        assertTrue(parsingResult.errors.isEmpty(), "Parsng errors\n" + parsingResult.errors)

        val antlrTree = parsingResult.root

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
                VariableDeclaration(BoolType(), "xb", BooleanLiteral(true)),
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
    fun parsesWhileStatement() {
        val code =  """
while (true) {
    int i = 0;
    break;
}
while (!flag)
    x = x + 42;
"""
        val expectedAst = Program(listOf(
                WhileStatement(BooleanLiteral(true),
                        StatementsBlock(listOf(
                                VariableDeclaration(IntType(), "i", IntLiteral(0)),
                                BreakStatement()
                        ))),
                WhileStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))))
        ))

        assertEquals(expectedAst, ast(code))
    }

    @Test
    fun parsesIfStatement() {
        val code =  """
if (flag1 && !flag2) {
    string s = "Hello";
    s2 = s + s2;
}
if (!flag)
    x = x + 42;
"""
        val expectedAst = Program(listOf(
                IfStatement(AndExpression(VariableReference("flag1"), NotExpression(VariableReference("flag2"))),
                        StatementsBlock(listOf(
                                VariableDeclaration(StringType(), "s", StringLiteral("Hello")),
                                Assignment("s2", AdditionExpression(VariableReference("s"), VariableReference("s2")))
                        )), null),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))), null)
        ))

        assertEquals(expectedAst, ast(code))
    }

    @Test
    fun parsesIfElseStatement() {
        val code =  """
if (flag1 && !flag2) {
    string s = "Hello";
    s2 = s + s2;
} else {
    string s = "Hello1";
    s2 = s + s2;
}
if (!flag)
    x = x + 42;
else
    x = x - 42;
"""
        val expectedAst = Program(listOf(
                IfStatement(AndExpression(VariableReference("flag1"), NotExpression(VariableReference("flag2"))),
                        StatementsBlock(listOf(
                                VariableDeclaration(StringType(), "s", StringLiteral("Hello")),
                                Assignment("s2", AdditionExpression(VariableReference("s"), VariableReference("s2")))
                        )),
                        StatementsBlock(listOf(
                                VariableDeclaration(StringType(), "s", StringLiteral("Hello1")),
                                Assignment("s2", AdditionExpression(VariableReference("s"), VariableReference("s2")))
                        ))),
                IfStatement(NotExpression(VariableReference("flag")),
                        Assignment("x", AdditionExpression(VariableReference("x"), IntLiteral(42))),
                        Assignment("x", SubtractionExpression(VariableReference("x"), IntLiteral(42))))
        ))

        // disabled ambiguity checks. Not sure if it is ok, but it seems to work as expected (dangling else)
        assertEquals(expectedAst, ast(code, diagnosticChecks = false))
    }

    @Test
    fun parsesNestedIfElseStatement() {
        val code =  """
if (flag1)
    x = 42;
else if (flag2)
    x = 43;
else
    x = 44;

if (flag1)
    if (flag2)
        x = 42;
    else
        x = 43;

if (flag1)
    if (flag2)
        x = 42;
    else
        x = 43;
else
    x = 44;
"""
        val expectedAst = Program(listOf(
                IfStatement(VariableReference("flag1"),
                        Assignment("x", IntLiteral(42)),
                        IfStatement(VariableReference("flag2"),
                                Assignment("x", IntLiteral(43)),
                                Assignment("x", IntLiteral(44)))),

                IfStatement(VariableReference("flag1"),
                        IfStatement(VariableReference("flag2"),
                                Assignment("x", IntLiteral(42)),
                                Assignment("x", IntLiteral(43))),
                        null),

                IfStatement(VariableReference("flag1"),
                        IfStatement(VariableReference("flag2"),
                                Assignment("x", IntLiteral(42)),
                                Assignment("x", IntLiteral(43))),
                        Assignment("x", IntLiteral(44)))
        ))

        // disabled ambiguity checks. Not sure if it is ok, but it seems to work as expected (dangling else)
        assertEquals(expectedAst, ast(code, diagnosticChecks = false))
    }

    @Test
    fun parsesComparisonExpressions() {
        val code =  """
if (a == 42 && b > 1 && c < 0 && d >= 10.5 && -1 <= e && str != "Hello") {
    f = x != 42;
}
"""
        val expectedAst = Program(listOf(
                IfStatement(
                        AndExpression(
                                AndExpression(
                                        AndExpression(
                                                AndExpression(
                                                        AndExpression(
                                                                EqualExpression(VariableReference("a"), IntLiteral(42)),
                                                                GreaterExpression(VariableReference("b"), IntLiteral(1))
                                                        ),
                                                        LessExpression(VariableReference("c"), IntLiteral(0))
                                                ),
                                                GreaterOrEqualExpression(VariableReference("d"), FloatLiteral(10.5))
                                        ),
                                        LessOrEqualExpression(UnaryMinusExpression(IntLiteral(1)), VariableReference("e"))
                                ),
                                NotEqualExpression(VariableReference("str"), StringLiteral("Hello"))
                        ),
                        StatementsBlock(listOf(
                                Assignment("f", NotEqualExpression(VariableReference("x"), IntLiteral(42)))
                        )), null)
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