package frontend.validation

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

        // disabled ambiguity checks (dangling else)
        assertEquals(expectedErrors, validate(code, diagnosticChecks = false))
    }
}
