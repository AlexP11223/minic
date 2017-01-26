package minic.backend.codegen

import minic.Compiler
import minic.JavaTestUtils
import minic.ProcessTestUtils
import org.apache.commons.io.FilenameUtils
import org.junit.*
import kotlin.test.*
import org.junit.rules.TemporaryFolder
import java.io.File

class CodeGenerationTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    private fun run(programPath: String): String {
        return ProcessTestUtils.run(JavaTestUtils.javaPath, args = FilenameUtils.getBaseName(programPath),
                workingDir = FilenameUtils.getFullPath(programPath))
    }

    private fun compileAndRun(code: String): String {
        val outputFilePath = tmpFolder.root.absolutePath + "/Program${System.currentTimeMillis()}_${code.length}.class"

        Compiler(diagnosticChecks = true).compile(code, outputFilePath)

        assertTrue(File(outputFilePath).exists(), "$outputFilePath doesn't exist")
        assertTrue(File(outputFilePath).length() > 0, "$outputFilePath is empty")

        return run(outputFilePath)
    }

    @Test
    fun printsStrings() {
        val code = """
println("Hello world!");
print("Hello ");
print("world.");
"""
        val expectedOutput = """
Hello world!
Hello world.
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }

    @Test
    fun printsNumbers() {
        val code = """
println(toString(42));
println(toString(42.5));
"""
        val expectedOutput = """
42
42.5
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }

    @Test
    fun printsBooleans() {
        val code = """
println(toString(true));
println(toString(false));
"""
        val expectedOutput = """
true
false
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }

    @Test
    fun concatenatesStrings() {
        val code = """
println("Hello " + "world!");
println("Hello" + " " + "world.");
"""
        val expectedOutput = """
Hello world!
Hello world.
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }

    @Test
    fun calculatesUnaryMinus() {
        val code = """
println(toString( -42 ));
println(toString( -42.5 ));
println(toString( --42.5 ));
println(toString( -(--42) ));
"""
        val expectedOutput = """
-42
-42.5
42.5
-42
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }

    @Test
    fun calculatesArithmeticExpressions() {
        val code = """
println(toString( 2 + 2 * 2 ));
println(toString( 4.5 * 2 ));
println(toString(  1 + 2 * 3/4.0 - (5 + 6 * 7 * (-8 - 9)) ));
println(toString( 8 % 2 ));
println(toString( 9 % 2 ));
println(toString( 9.5 % 2 ));
int a = 2;
println(toString( 2 + a * 2 ));
double b = 2.0;
println(toString( 2 + b * 2 ));
"""
        val expectedOutput = """
6
9.0
711.5
0
1
1.5
6
6.0
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }

    @Test
    fun calculatesLogicalExpressions() {
        val code = """
bool a = true;
bool b = false;
println(toString( a && a ) + " " + toString( a && b ) + " " + toString( b && a ) + " " + toString( b && b ));
println(toString( a || a ) + " " + toString( a || b ) + " " + toString( b || a ) + " " + toString( b || b ));
println(toString( !a ) + " " + toString( !b ) + " " + toString( !!a ));
println(toString( a && (a || b) ));
println(toString( a && (!a || b) ));
"""
        val expectedOutput = """
true false false false
true true true false
false true true
true
false
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }

    @Test
    fun handlesVariables() {
        val code = """
int a = 0;
{
    double b = 1.0;
    int c = 2;
    string d = "hello";
    {
        double e = 4.5;
        int f = 5;

        println(toString(b));
        println(toString(c));
        println(d);
        println(toString(e));
        println(toString(f));
    }
    c = 42;
    int e = a + 6;

    println(toString(e));
    println(toString(c));
}
int b = 7;
double c = 8;
{
    int d = 9;
    println(toString(d));
}
bool e = true;

println(toString(a));
println(toString(b));
println(toString(c));
println(toString(e));
"""
        val expectedOutput = """
1.0
2
hello
4.5
5
6
42
9
0
7
8.0
true
""".trim()
        assertEquals(expectedOutput, compileAndRun(code))
    }
}
