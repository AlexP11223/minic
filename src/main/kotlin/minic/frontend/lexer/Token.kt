package minic.frontend.lexer

// TODO: probably should add int/enum type value instead of just name
data class Token(val startIndex: Int, val endIndex: Int, val line: Int, val text: String, val name: String) {
    val length = endIndex - startIndex + 1;
}
