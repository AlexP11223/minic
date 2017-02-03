package minic

import minic.backend.ExecutionRuntimeException
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
}
