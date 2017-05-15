package minic

import minic.backend.ExecutionRuntimeException
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertTrue

class CompilerTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    @Test
    fun producesOutputFile() {
        val outputFilePath = tmpFolder.root.absolutePath + "/Program.class"

        Compiler(input = "").compile(outputFilePath)

        assertTrue(File(outputFilePath).exists(), "$outputFilePath doesn't exist")
        assertTrue(File(outputFilePath).length() > 0, "$outputFilePath is empty")
    }

    @Test
    fun executesWithoutFail() {
        Compiler(input = "").execute()
    }

    @Test(expected = ExecutionRuntimeException::class)
    fun throwsWhenExecutionRuntimeError() {
        Compiler(input = "int a = 1/0;").execute()
    }

    @Test
    fun generatesBytecodeText() {
        val code = """
int x = 42;
if (x > 0)
    x = x + 1;
else
    x = x - 1;
""".trim()
        val result = Compiler(input = code).bytecodeText()

        assertThat(result, containsString("LDC 42"))
        assertThat(result, containsString("ISTORE"))
        assertThat(result, containsString("ILOAD"))
        assertThat(result, containsString("LDC 1"))
        assertThat(result, containsString("IADD"))
        assertThat(result, not(containsString("FRAME")))
    }

    @Test
    fun decompilesBytecodeText() {
        val code = """
int x = 42;
if (x > 0)
    x = x + 1;
else
    x = x - 1;
""".trim()
        val result = Compiler(input = code).decompileBytecodeText()

        assertThat(result, containsString("LDC 42"))
        assertThat(result, containsString("ISTORE"))
        assertThat(result, containsString("ILOAD"))
        assertThat(result, containsString("LDC 1"))
        assertThat(result, containsString("IADD"))
        assertThat(result, containsString("FRAME"))
    }
}
