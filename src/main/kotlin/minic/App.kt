package minic

import joptsimple.OptionParser
import joptsimple.OptionSpec
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.PrintStream

internal class App(val out: PrintStream = System.out,
                   val err: PrintStream = System.err,
                   val input: InputStream = System.`in`) {

    private val compilerName = "minic"

    private val optionParser = OptionParser()
    private val helpOption: OptionSpec<Void>
    private val astOption: OptionSpec<String>
    private val bytecodeOption: OptionSpec<Void>
    private val decompiledBytecodeOption: OptionSpec<Void>
    private val tokensOption: OptionSpec<Void>

    init {
        val newline = System.lineSeparator() + "        "

        helpOption = optionParser.acceptsAll(arrayListOf("h", "help", "?"), "Show help").forHelp()
        astOption = optionParser.accepts("ast", "Draw AST and save as PNG image (default ast.png)")
                .withOptionalArg().defaultsTo("ast.png").describedAs("png_output_file")
        bytecodeOption = optionParser.accepts("bytecode", "Output bytecode as text (only the main code, not the whole${newline}generated class, and without frames map)")
        decompiledBytecodeOption = optionParser.accepts("decompiled_bytecode", "The same as --bytecode but extracts bytecode from generated result${newline}instead of writing it during codegen, includes frames map")
        tokensOption = optionParser.accepts("tokens", "Output lexer tokens")
    }

    fun run(args: Array<String>) {
        val options = try {
            optionParser.parse(*args)
        } catch (ex: Exception) {
            err.println(ex.message)
            showUsage()
            return
        }

        val nonOptionArgs = options.nonOptionArguments().map { it.toString() }

        if (options.has(helpOption) || nonOptionArgs.contains("help")) {
            showUsage()
            return
        }

        if (nonOptionArgs.count() > 2) {
            err.println("Incorrect arguments")
            showUsage()
            return
        }

        val executionMode = nonOptionArgs.isEmpty()

        var inputStream = input
        var outputFilePath: String? = null

        if (nonOptionArgs.any()) {
            val inputFilePath = nonOptionArgs[0]

            outputFilePath = if (nonOptionArgs.count() > 1) nonOptionArgs[1] else FilenameUtils.removeExtension(inputFilePath)
            if (!outputFilePath!!.endsWith(".class"))
                outputFilePath += ".class"

            out.println("Input file: $inputFilePath")
            out.println("Output file: $outputFilePath")

            if (!File(inputFilePath).isFile) {
                err.println("File not found.")
                return
            }

            inputStream = FileInputStream(inputFilePath)
        }

        if (executionMode) {
            out.println("Enter code and press Ctrl+D (Ctrl+Z for Windows)")
            out.println()
        }

        val compiler = Compiler(inputStream)

        val errors = compiler.validate()
        if (errors.any()) {
            out.println("${errors.count()} error${if (errors.count() > 1) "s" else ""}.")
            errors.forEach {
                out.println("Line ${it.position.line}:${it.position.column}: ${it.message}")
            }
            return
        }

        try {
            if (executionMode) {
                compiler.execute()
            } else {
                compiler.compile(outputFilePath!!)
            }
        } catch (ex: Exception) {
            err.println("Code generation error")
            err.println(ex.message)
            ex.printStackTrace()
            return
        }

        if (options.has(tokensOption)) {
            out.println("Tokens:")
            compiler.tokens.forEach {
                out.println("${it.name}: ${it.text}")
            }
        }

        if (options.has(bytecodeOption)) {
            out.println("Bytecode:")
            out.println(compiler.bytecodeText())
        }

        if (options.has(decompiledBytecodeOption)) {
            out.println("Decompiled bytecode:")
            out.println(compiler.decompileBytecodeText())
        }

        if (options.has(astOption)) {
            val astOutputFilePath = astOption.value(options)
            compiler.drawAst(astOutputFilePath)
            out.println("Saved AST to $astOutputFilePath")
        }
    }


    private fun showUsage() {
        err.println(
                """Usage:
$compilerName [input_file [output_file]] [options]
    Compiles specified source code into file with JVM bytecode.
    input_file
        Path (or name) of file with Mini-C source code.
    output_file
        Optional. Path (or name) of output file with JVM bytecode.
        If not specified, input_file without extension will be used.
        .class extension is appended if not present (otherwise java will
         not run it), such as MyProgram.class.
    Use java to run it (java MyProgram).
If launched without these arguments, reads input from stdin
until EOF (Ctrl+D, or Ctrl+Z for Windows), compiles and runs the program.""")

        err.println("Options:")
        val indent = "    "
        optionParser.formatHelpWith({ optionsDescrMap ->
            optionsDescrMap.values.distinct()
                    .filter { !it.representsNonOptions() }
                    .map {
                         indent + it.options()
                                 .map {
                                     val prefix = if (it.length > 1) "--" else "-"
                                     prefix + it
                                 }.joinToString(", ") +
                                 (if (it.acceptsArguments())
                                     (" " + if (it.requiresArgument()) "<${it.argumentDescription()}>" else "[${it.argumentDescription()}]")
                                 else "") +
                                 System.lineSeparator() + indent + indent + it.description()
                    }
                    .joinToString(System.lineSeparator()) + System.lineSeparator()
        })
        optionParser.printHelpOn(err)

        err.println("""Examples:
    $compilerName MyProgram.mc
    $compilerName MyProgram.mc MyProgram
    $compilerName MyProgram.mc --tokens
    $compilerName MyProgram.mc --ast myast.png
    $compilerName MyProgram.mc --bytecode
    $compilerName MyProgram.mc --decompiled_bytecode""")
    }


}
