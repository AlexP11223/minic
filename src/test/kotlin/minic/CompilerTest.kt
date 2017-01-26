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

        Compiler().compile("", outputFilePath)

        assertTrue(File(outputFilePath).exists(), "$outputFilePath doesn't exist")
        assertTrue(File(outputFilePath).length() > 0, "$outputFilePath is empty")
    }

    @Test
    fun executesWithoutFail() {
        Compiler().execute("")
    }

    @Test(expected = ExecutionRuntimeException::class)
    fun throwsWhenExecutionRuntimeError() {
        Compiler().execute("int a = 1/0;")
    }
}
