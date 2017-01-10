package minic

import minic.frontend.antlr.MiniCLexer
import minic.frontend.antlr.MiniCParser
import minic.frontend.ast.AntlrToAstMapper
import minic.frontend.ast.Point
import minic.frontend.ast.Program
import minic.frontend.validation.Error
import minic.frontend.validation.validate
import org.antlr.v4.runtime.*
import java.io.InputStream
import java.util.*

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
        lexer.removeErrorListeners(); // remove ConsoleErrorListener
        lexer.addErrorListener(errorListener)

        val tokens = CommonTokenStream(lexer)

        val parser = MiniCParser(tokens)
        parser.removeErrorListeners(); // remove ConsoleErrorListener
        parser.addErrorListener(errorListener)
        if (diagnosticChecks) {
            parser.addErrorListener(DiagnosticErrorListener())
        }

        return AntlrParsingResult(parser.program(), errors)
    }

    fun validate(input: String) : List<Error> = validate(ANTLRInputStream(input))
    fun validate(input: InputStream) : List<Error> = validate(ANTLRInputStream(input))
    fun validate(input: ANTLRInputStream) : List<Error> = validate(parse(input))

    fun validate(parsingResult: AntlrParsingResult) : List<Error> {
        if (parsingResult.errors.any())
            return parsingResult.errors

        val mapper = AntlrToAstMapper()
        val ast = mapper.map(parsingResult.root)

        return ast.validate()
    }

    fun compile(input: ANTLRInputStream) {
        val parsingResult = parse(input)

        val errors = validate(parsingResult)
        if (errors.any())
            throw Exception(errors.joinToString())


    }
}
