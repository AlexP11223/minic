package minic.frontend.ast

// base interface for all AST nodes
interface AstNode {
    val position: Position? // should not really be nullable, needed only to simplify some parsing tests

    // no children by default
    fun children() : List<AstNode> = listOf()

    fun process(operation: (AstNode) -> Unit) {
        operation(this);

        children().forEach {
            it.process(operation)
        }
    }
}
