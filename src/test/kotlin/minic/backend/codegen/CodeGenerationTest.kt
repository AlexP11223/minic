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

}
