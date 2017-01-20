package minic.frontend.validation

import minic.Compiler
import minic.frontend.ast.Point
import minic.frontend.validation.Error
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationTest {

    fun validate(code: String, diagnosticChecks: Boolean = true): List<Error> {
        return Compiler(diagnosticChecks).validate(code)
    }

    @Test
    fun reportsSyntaxErrors() {
        val code = """
qwe;
abc/
""".trim()

        val errors = validate(code)

        assertTrue(errors.count() >= 2)
    }

    @Test
    fun allowsBreaksOnlyInWhile() {
        val code = """
break;
while (true) {
    if (true)
        break;
    break;
}
if (true)
    break;
if (true) {
    break;
}
""".trim()

        val expectedErrors = listOf(
                Error("Unexpected break statement", Point(1, 0)),
                Error("Unexpected break statement", Point(8, 4)),
                Error("Unexpected break statement", Point(10, 4))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun dontAllowVarDeclarationInNonBlockIfWhile() {
        val code = """
int a = 42;
while (true)
    int b = 42;
while (true) {
    int c = 42;
    if (true)
        double d = 42.0;
    else
        string e = "42";
}
if (true)
    bool f = true;
else
    int g = 42;
if (true) {
    int h = 42;
} else {
    int i = 42;
}
""".trim()

        val expectedErrors = listOf(
                Error("Variable declaration not allowed here", Point(3, 4)),
                Error("Variable declaration not allowed here", Point(7, 8)),
                Error("Variable declaration not allowed here", Point(9, 8)),
                Error("Variable declaration not allowed here", Point(12, 4)),
                Error("Variable declaration not allowed here", Point(14, 4))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun detectUndeclaredVariables() {
        val code = """
a = 42;
while (true)
    b = 42;
while (true) {
    if (true)
        c = 42.0;
    else {
        string d = e;
    }
}
f = g + h;
""".trim()

        val expectedErrors = listOf(
                Error("Variable 'a' is not declared", Point(1, 0)),
                Error("Variable 'b' is not declared", Point(3, 4)),
                Error("Variable 'c' is not declared", Point(6, 8)),
                Error("Variable 'e' is not declared", Point(8, 19)),
                Error("Variable 'f' is not declared", Point(11, 0)),
                Error("Variable 'g' is not declared", Point(11, 4)),
                Error("Variable 'h' is not declared", Point(11, 8))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun detectRedeclaredVariables() {
        val code = """
int a = 42;
int a = 43;
while (true) {
    bool a = true;
    if (true) {
        double a = 42.0;
    }
    else {
        string a = "Hello";
    }
}
""".trim()

        val expectedErrors = listOf(
                Error("Variable 'a' is already declared", Point(2, 0)),
                Error("Variable 'a' is already declared", Point(4, 4)),
                Error("Variable 'a' is already declared", Point(6, 8)),
                Error("Variable 'a' is already declared", Point(9, 8))
        )

        assertEquals(expectedErrors, validate(code))
    }
}
