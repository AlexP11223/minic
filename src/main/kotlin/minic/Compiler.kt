package minic

import minic.backend.codegen.jvm.JvmCodeGenerator
import minic.backend.codegen.jvm.info.BytecodeDecompiler
import minic.backend.info.tree.AstGraphvizRenderer
import minic.backend.info.tree.TreePainter
import minic.frontend.antlr.MiniCLexer
import minic.frontend.antlr.MiniCParser
import minic.frontend.ast.AntlrToAstMapper
import minic.frontend.ast.Point
import minic.frontend.ast.Program
import minic.frontend.lexer.Token
import minic.frontend.validation.Error
import minic.frontend.validation.validate
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.PredictionMode
import org.apache.commons.io.FilenameUtils
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * @param diagnosticChecks Enables additional checks during parsing (ambiguity, ...) and code generation (bytecode correctness).
 * @param debugInfo Adds additional information, such as source code line numbers in bytecode
 */
data class CompilerConfiguration(val diagnosticChecks: Boolean = false, val debugInfo: Boolean = false)

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

    /**
     * Tokens recognized by lexer
     */
    val tokens: List<Token> by lazy {
        input.reset()
        val lexer = MiniCLexer(input)
        val tokensList = ArrayList<Token>()
        do {
            val t = lexer.nextToken()
            val name = when (t.type) {
                -1 -> "EOF"
                else -> MiniCLexer.VOCABULARY.getSymbolicName(t.type)
            }
            tokensList.add(Token(t.startIndex, t.stopIndex, t.line, t.text, name))
        } while (t.type != -1)
        tokensList
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

    private fun generateJvmBytecode(className: String) : JvmCodeGenerator {
        val errors = validate()
        if (errors.any())
            throw Exception(errors.joinToString())

        return JvmCodeGenerator(ast, className, diagnosticChecks = config.diagnosticChecks, debugInfo = config.debugInfo)
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

    /**
     * Renders AST to an image
     */
    fun drawAst(painter: TreePainter? = null) : BufferedImage {
        if (parsingResult.errors.any())
            throw Exception(parsingResult.errors.joinToString())

        return AstGraphvizRenderer(ast, painter).render()
    }

    /**
     * Renders AST to an image and saves it to the specified file (PNG)
     */
    fun drawAst(outputFilePath: String, painter: TreePainter? = null) {
        ImageIO.write(drawAst(painter), "png", File(outputFilePath))
    }

    fun bytecodeText(): String {
        return generateJvmBytecode("MinicMain").bytecodeText
    }

    fun decompileBytecodeText(): String {
        return decompileBytecodeText(generateJvmBytecode("MinicMain").bytes)
    }

    fun decompileBytecodeText(classBytes: ByteArray): String {
        return BytecodeDecompiler(classBytes).methodText("execute", showFrames = true)
    }
}
