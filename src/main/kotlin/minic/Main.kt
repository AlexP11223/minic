package minic

import minic.frontend.antlr.MiniCLexer
import minic.frontend.antlr.MiniCParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

fun main(args: Array<String>) {
    println("Mini-C compiler")
    println("Enter code and press Ctrl+D (Ctrl+Z for Windows)")
    println()

    val input = ANTLRInputStream(System.`in`)

    val lexer = MiniCLexer(input)

    val tokens = CommonTokenStream(lexer)

    val parser = MiniCParser(tokens)

    parser.program()
}
