package minic.frontend.validation

import minic.Compiler
import minic.frontend.ast.Point
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
    fun doesntAllowVarDeclarationInNonBlockIfWhile() {
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
    fun detectsUndeclaredVariables() {
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
if (true) {
    int x = 42;
}
if (true) {
    print(toString(x + 1));
}
""".trim()

        val expectedErrors = listOf(
                Error("Variable 'a' is not declared", Point(1, 0)),
                Error("Variable 'b' is not declared", Point(3, 4)),
                Error("Variable 'c' is not declared", Point(6, 8)),
                Error("Variable 'e' is not declared", Point(8, 19)),
                Error("Variable 'f' is not declared", Point(11, 0)),
                Error("Variable 'g' is not declared", Point(11, 4)),
                Error("Variable 'h' is not declared", Point(11, 8)),
                Error("Variable 'x' is not declared", Point(16, 19))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun detectsRedeclaredVariables() {
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

    @Test
    fun detectsIncompatibleVarDeclarationType() {
        val code = """
string a = 42;
bool b = 42;
int c = 42.5;
double d = "hello";
int i = 0;
string e = i;
while (true) {
    bool f = "hello";
}
""".trim()

        val expectedErrors = listOf(
                Error("Cannot assign expression of type 'int' to a variable of type 'string'", Point(1, 0)),
                Error("Cannot assign expression of type 'int' to a variable of type 'bool'", Point(2, 0)),
                Error("Cannot assign expression of type 'double' to a variable of type 'int'", Point(3, 0)),
                Error("Cannot assign expression of type 'string' to a variable of type 'double'", Point(4, 0)),
                Error("Cannot assign expression of type 'int' to a variable of type 'string'", Point(6, 0)),
                Error("Cannot assign expression of type 'string' to a variable of type 'bool'", Point(8, 4))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun detectsIncompatibleVarAssignmentType() {
        val code = """
int i = 0;
double fnum = 0.0;
string str = "Hello";
bool flag = true;

i = 42.5;
flag = 42;
str = 42;
fnum = "hello";
str = i;
while (true) {
    flag = "hello";
}
""".trim()

        val expectedErrors = listOf(
                Error("Cannot assign expression of type 'double' to a variable of type 'int'", Point(6, 0)),
                Error("Cannot assign expression of type 'int' to a variable of type 'bool'", Point(7, 0)),
                Error("Cannot assign expression of type 'int' to a variable of type 'string'", Point(8, 0)),
                Error("Cannot assign expression of type 'string' to a variable of type 'double'", Point(9, 0)),
                Error("Cannot assign expression of type 'int' to a variable of type 'string'", Point(10, 0)),
                Error("Cannot assign expression of type 'string' to a variable of type 'bool'", Point(12, 4))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun allowsOnlyBooleanIfExpr() {
        val code = """
if (1) { }
if (42.5) { }
if ("hello") {
    int i = 0;
    if (i) { }
    else if (i+i) { }
}
""".trim()

        val expectedErrors = listOf(
                Error("Expression must be 'bool', got 'int'", Point(1, 4)),
                Error("Expression must be 'bool', got 'double'", Point(2, 4)),
                Error("Expression must be 'bool', got 'string'", Point(3, 4)),
                Error("Expression must be 'bool', got 'int'", Point(5, 8)),
                Error("Expression must be 'bool', got 'int'", Point(6, 13))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun allowsOnlyBooleanWhileExpr() {
        val code = """
while (1) { }
while (42.5) { }
while ("hello") {
    int i = 0;
    while (i) { }
}
""".trim()

        val expectedErrors = listOf(
                Error("Expression must be 'bool', got 'int'", Point(1, 7)),
                Error("Expression must be 'bool', got 'double'", Point(2, 7)),
                Error("Expression must be 'bool', got 'string'", Point(3, 7)),
                Error("Expression must be 'bool', got 'int'", Point(5, 11))
        )

        assertEquals(expectedErrors, validate(code))
    }

    @Test
    fun printAcceptsOnlyStringExpr() {
        val code = """
string name = "Alice";
int age = 10;
double f = 10.5;
bool flag = true;

println("Hello " + name);
print(name);
print(toString(age));

print(age);
println(age);
print(f);
println(f);
print(flag);
println(flag);
if (age < 18) {
    print(age);
}
""".trim()

        val expectedErrors = listOf(
                Error("Expression must be 'string', got 'int'", Point(10, 6)),
                Error("Expression must be 'string', got 'int'", Point(11, 8)),
                Error("Expression must be 'string', got 'double'", Point(12, 6)),
                Error("Expression must be 'string', got 'double'", Point(13, 8)),
                Error("Expression must be 'string', got 'bool'", Point(14, 6)),
                Error("Expression must be 'string', got 'bool'", Point(15, 8)),
                Error("Expression must be 'string', got 'int'", Point(17, 10))
        )

        assertEquals(expectedErrors, validate(code))
    }
}
