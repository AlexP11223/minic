package minic

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.test.assertTrue

class AppTest {
    var output: String = ""
    var errorOutput: String = ""

    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    private fun run(args: Array<String>, input: String = "") {
        val inBaos = ByteArrayOutputStream()
        val errBaos = ByteArrayOutputStream()
        val inputStream = if (input.isEmpty()) System.`in` else IOUtils.toInputStream(input, StandardCharsets.UTF_8)

        App(PrintStream(inBaos), PrintStream(errBaos), inputStream).run(args)

        output = String(inBaos.toByteArray(), StandardCharsets.UTF_8)
        errorOutput = String(errBaos.toByteArray(), StandardCharsets.UTF_8)
    }

    private fun assertShowsHelp() {
        assertThat(errorOutput, containsString("Usage:"))
        assertThat(errorOutput, containsString("Options:"))
        assertThat(errorOutput, containsString("--ast"))
        assertThat(errorOutput, containsString("--tokens"))
        assertThat(errorOutput, containsString("Examples:"))
    }

    @Test
    fun showsHelpWhenIncorrectOption() {
        run(arrayOf("--unknownflag"))

        assertThat(errorOutput, containsString("unknownflag"))
        assertThat(errorOutput, containsString("recognized"))
        assertShowsHelp()

        run(arrayOf("-q"))

        assertThat(errorOutput, containsString("q"))
        assertThat(errorOutput, containsString("recognized"))
        assertShowsHelp()
    }

    @Test
    fun showsHelpWhenTooMuchArgs() {
        run(arrayOf("arg1", "arg2", "arg3"))

        assertShowsHelp()
    }

    @Test
    fun showsErrorWhenInputFileNotFound() {
        run(arrayOf("notexisting.mc"))

        assertThat(errorOutput, containsString("not found"))
    }

    @Test
    fun compilesToDefaultOutputPath() {
        val inputFilePath = tmpFolder.root.absolutePath + "/Program.mc"
        val outputFilePath = tmpFolder.root.absolutePath + "/Program.class"

        FileUtils.writeStringToFile(File(inputFilePath), "print(\"Hello\");", Charset.defaultCharset())

        run(arrayOf(inputFilePath))

        assertTrue(errorOutput.isEmpty(), errorOutput)
        assertTrue(File(outputFilePath).exists(), "$outputFilePath doesn't exist")
        assertTrue(File(outputFilePath).length() > 0, "$outputFilePath is empty")
    }

    @Test
    fun compilesToCustomOutputPath() {
        val inputFilePath = tmpFolder.root.absolutePath + "/Program.mc"
        val outputFilePath = tmpFolder.root.absolutePath + "/MyProgram.class"

        FileUtils.writeStringToFile(File(inputFilePath), "print(\"Hello\");", Charset.defaultCharset())

        run(arrayOf(inputFilePath, outputFilePath))

        assertTrue(errorOutput.isEmpty(), errorOutput)
        assertTrue(File(outputFilePath).exists(), "$outputFilePath doesn't exist")
        assertTrue(File(outputFilePath).length() > 0, "$outputFilePath is empty")
    }

    @Test
    fun executesFromInput() {
        run(arrayOf(), "int x = 42;")

        assertTrue(errorOutput.isEmpty(), errorOutput)

        assertThat(output, containsString("Ctrl+D"))
    }

    @Test
    fun compilesAndShowsTokensAstBytecode() {
        val inputFilePath = tmpFolder.root.absolutePath + "/Program.mc"
        val astOutputFilePath = tmpFolder.root.absolutePath + "/ast.png"

        FileUtils.writeStringToFile(File(inputFilePath), "int x = 42;\n int y = x*2;", Charset.defaultCharset())

        val argsCombinations = listOf(
                arrayOf(inputFilePath, "--tokens", "--bytecode", "--ast", astOutputFilePath),
                arrayOf(inputFilePath, "--ast", astOutputFilePath, "--tokens", "--bytecode")
        )

        argsCombinations.forEach { args ->
            run(args)

            assertTrue(errorOutput.isEmpty(), errorOutput)

            assertThat(output, containsString("Tokens:"))
            assertThat(output, containsString("INT_TYPE"))
            assertThat(output, containsString("ASSIGN"))
            assertThat(output, containsString("MUL"))

            assertThat(output, containsString("Bytecode:"))
            assertThat(output, containsString("ISTORE"))
            assertThat(output, containsString("IMUL"))

            assertTrue(File(astOutputFilePath).exists(), "$astOutputFilePath doesn't exist")
            assertTrue(File(astOutputFilePath).length() > 0, "$astOutputFilePath is empty")

            File(astOutputFilePath).delete()
        }
    }

    @Test
    fun executesAndShowsTokensAstBytecode() {
        val astOutputFilePath = tmpFolder.root.absolutePath + "/ast.png"

        run(arrayOf("--tokens", "--bytecode", "--ast", astOutputFilePath), "int x = 42;\n int y = x*2;")

        assertTrue(errorOutput.isEmpty(), errorOutput)

        assertThat(output, containsString("Tokens:"))
        assertThat(output, containsString("INT_TYPE"))
        assertThat(output, containsString("ASSIGN"))
        assertThat(output, containsString("MUL"))

        assertThat(output, containsString("Bytecode:"))
        assertThat(output, containsString("ISTORE"))
        assertThat(output, containsString("IMUL"))

        assertTrue(File(astOutputFilePath).exists(), "$astOutputFilePath doesn't exist")
        assertTrue(File(astOutputFilePath).length() > 0, "$astOutputFilePath is empty")
    }

    @Test
    fun showsCompilationErrors() {
        val inputFilePath = tmpFolder.root.absolutePath + "/Program.mc"
        val outputFilePath = tmpFolder.root.absolutePath + "/Program.class"

        FileUtils.writeStringToFile(File(inputFilePath), "int a = b;", Charset.defaultCharset())

        run(arrayOf(inputFilePath))

        assertTrue(errorOutput.isEmpty(), errorOutput)

        assertThat(output, containsString("error"))
        assertThat(output, containsString("declared"))

        assertTrue(!File(outputFilePath).exists(), "$outputFilePath exists")
    }
}
