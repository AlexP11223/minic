package frontend.validation

import minic.Compiler
import minic.frontend.ast.Point
import minic.frontend.validation.Error
import org.junit.Test
import kotlin.test.assertEquals

class ValidationTest {

    fun validate(code: String, diagnosticChecks: Boolean = true): List<Error> {
        return Compiler(diagnosticChecks).validate(code)
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

        assertEquals(listOf(
                Error("Unexpected break statement", Point(1, 0)),
                Error("Unexpected break statement", Point(8, 4)),
                Error("Unexpected break statement", Point(10, 4))
                ), validate(code))
    }
}
