package minic.frontend.ast

// base interface for all AST nodes
interface AstNode {
    val position: Position? // should not really be nullable, needed only to simplify some parsing tests

    // no children by default
    fun children() : List<AstNode> = listOf()

    fun process(operation: (AstNode) -> Unit) {
        operation(this)

        children().forEach {
            it.process(operation)
        }
    }

    fun <T: AstNode> process(nodeClass: Class<T>, operation: (T) -> Unit) {
        process {
            if (nodeClass.isInstance(it)) {
                operation(it as T)
            }
        }
    }

    fun process(nodeClasses: List<Class<out AstNode>>, operation: (AstNode) -> Unit) {
        process { node ->
            if (nodeClasses.any { it.isInstance(node) }) {
                operation(node)
            }
        }
    }

    fun processUntil(operation: (AstNode) -> Boolean) {
        if (operation(this)) {
            children().forEach {
                it.processUntil(operation)
            }
        }
    }
}
