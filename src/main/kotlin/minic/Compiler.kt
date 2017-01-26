package minic

import minic.backend.codegen.jvm.JvmCodeGenerator
import minic.frontend.antlr.MiniCLexer
import minic.frontend.antlr.MiniCParser
import minic.frontend.ast.AntlrToAstMapper
import minic.frontend.ast.Point
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
class Compiler(val diagnosticChecks: Boolean = false) {

    data class AntlrParsingResult(val root: MiniCParser.ProgramContext, val errors: List<Error>)

    fun parse(input: String) : AntlrParsingResult = parse(ANTLRInputStream(input))
    fun parse(input: InputStream) : AntlrParsingResult = parse(ANTLRInputStream(input))

    fun parse(input: ANTLRInputStream) : AntlrParsingResult {
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
        if (diagnosticChecks) {
            parser.interpreter.predictionMode = PredictionMode.LL_EXACT_AMBIG_DETECTION
            parser.addErrorListener(DiagnosticErrorListener())
        }

        return AntlrParsingResult(parser.program(), errors)
    }

    fun validate(input: String) : List<Error> = validate(ANTLRInputStream(input))
    fun validate(input: InputStream) : List<Error> = validate(ANTLRInputStream(input))
    fun validate(input: ANTLRInputStream) : List<Error> = validate(parse(input))

    /**
     * Returns list of errors (syntax or semantic), or empty list if there are no errors
     */
    fun validate(parsingResult: AntlrParsingResult) : List<Error> {
        if (parsingResult.errors.any())
            return parsingResult.errors

        val ast = AntlrToAstMapper().map(parsingResult.root)

        return ast.validate()
    }

    private fun generateJvmBytecode(parsingResult: AntlrParsingResult, classNane: String) : JvmCodeGenerator {
        val errors = validate(parsingResult)
        if (errors.any())
            throw Exception(errors.joinToString())

        val ast = AntlrToAstMapper().map(parsingResult.root)

        return JvmCodeGenerator(ast, classNane, diagnosticChecks)
    }

    fun compile(input: String, outputFilePath: String) = compile(ANTLRInputStream(input), outputFilePath)
    fun compile(input: InputStream, outputFilePath: String) = compile(ANTLRInputStream(input), outputFilePath)
    fun compile(input: ANTLRInputStream, outputFilePath: String) = compile(parse(input), outputFilePath)

    fun compile(parsingResult: AntlrParsingResult, outputFilePath: String) {
        val className = FilenameUtils.removeExtension(FilenameUtils.getName(outputFilePath))

        val  bytes = generateJvmBytecode(parsingResult, className).bytes

        FileOutputStream(outputFilePath).use {
            it.write(bytes)
        }
    }

    fun execute(input: String) = execute(ANTLRInputStream(input))
    fun execute(input: InputStream) = execute(ANTLRInputStream(input))
    fun execute(input: ANTLRInputStream) = execute(parse(input))

    /**
     * Executes program in current thread
     */
    fun execute(parsingResult: AntlrParsingResult) {
        generateJvmBytecode(parsingResult, "MinicMain").execute()
    }
}
