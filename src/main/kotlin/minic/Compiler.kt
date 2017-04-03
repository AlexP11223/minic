package minic

import minic.backend.codegen.jvm.JvmCodeGenerator
import minic.frontend.antlr.MiniCLexer
import minic.frontend.antlr.MiniCParser
import minic.frontend.ast.AntlrToAstMapper
import minic.frontend.ast.Point
import minic.frontend.ast.Program
import minic.frontend.validation.Error
import minic.frontend.validation.validate
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.PredictionMode
import org.apache.commons.io.FilenameUtils
import java.io.FileOutputStream
import java.io.InputStream

/**
 * @param diagnosticChecks Enables additional checks during parsing (ambiguity, ...) and code generation (bytecode correctness).
 */
data class CompilerConfiguration(val diagnosticChecks: Boolean = false)

class Compiler internal constructor(private val input: ANTLRInputStream, val config: CompilerConfiguration = CompilerConfiguration()) {
    constructor(input: String, config: CompilerConfiguration = CompilerConfiguration()) : this(ANTLRInputStream(input), config)
    constructor(input: InputStream, config: CompilerConfiguration = CompilerConfiguration()) : this(ANTLRInputStream(input), config)

    data class AntlrParsingResult(internal val root: MiniCParser.ProgramContext, val errors: List<Error>)

    /**
     * ANTLR tree and errors produced after parsing
     */
    val parsingResult: AntlrParsingResult

    init {
        parsingResult = parse(input)
    }

    /**
     * AST of the program code
     * Computed only once on the first access
     * May throw exception if [parsingResult] has errors
     */
    val ast: Program by lazy(LazyThreadSafetyMode.NONE) {
        AntlrToAstMapper().map(parsingResult.root)
    }

    private fun parse(input: ANTLRInputStream) : AntlrParsingResult {
        val errors = mutableListOf<Error>()

        val errorListener = object : BaseErrorListener() {

            override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInline: Int, msg: String, ex: RecognitionException?) {
                errors.add(Error(msg, Point(line, charPositionInline)))
            }
        }

        val lexer = MiniCLexer(input)
        lexer.removeErrorListeners() // remove ConsoleErrorListener
        lexer.addErrorListener(errorListener)

        val tokens = CommonTokenStream(lexer)

        val parser = MiniCParser(tokens)
        parser.removeErrorListeners() // remove ConsoleErrorListener
        parser.addErrorListener(errorListener)
        if (config.diagnosticChecks) {
            parser.interpreter.predictionMode = PredictionMode.LL_EXACT_AMBIG_DETECTION
            parser.addErrorListener(DiagnosticErrorListener())
        }

        return AntlrParsingResult(parser.program(), errors)
    }

    /**
     * Returns list of errors (syntax or semantic), or empty list if there are no errors
     */
    fun validate() : List<Error> {
        if (parsingResult.errors.any())
            return parsingResult.errors

        return ast.validate()
    }

    private fun generateJvmBytecode(classNane: String) : JvmCodeGenerator {
        val errors = validate()
        if (errors.any())
            throw Exception(errors.joinToString())

        return JvmCodeGenerator(ast, classNane, config.diagnosticChecks)
    }

    /**
     * Creates file with JVM bytecode
     * @param outputFilePath Path of the output file with JVM bytecode.
     * The file name should have .class extension to be able to be executed by java
     */
    fun compile(outputFilePath: String) {
        val className = FilenameUtils.removeExtension(FilenameUtils.getName(outputFilePath))

        val  bytes = generateJvmBytecode(className).bytes

        FileOutputStream(outputFilePath).use {
            it.write(bytes)
        }
    }

    /**
     * Executes program in current thread
     */
    fun execute() {
        generateJvmBytecode("MinicMain").execute()
    }
}
