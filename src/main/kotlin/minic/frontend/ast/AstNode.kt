package minic.frontend.ast

// base interface for all AST nodes
interface AstNode {
    val position: Position? // should not really be nullable, needed only to simplify some parsing tests

    // no children by default
    fun children() : List<AstNode> = listOf()

    fun process(enterOperation: (AstNode) -> Unit, exitOperation: (AstNode) -> Unit) {
        enterOperation(this)

        children().forEach {
            it.process(enterOperation, exitOperation)
        }

        exitOperation(this)
    }

    fun <T: AstNode> process(nodeClass: Class<T>, enterOperation: (T) -> Unit, exitOperation: (T) -> Unit) {
        process(enterOperation = {
            if (nodeClass.isInstance(it)) {
                enterOperation(it as T)
            }
        }, exitOperation = {
            if (nodeClass.isInstance(it)) {
                exitOperation(it as T)
            }
        })
    }

    fun process(operation: (AstNode) -> Unit) = process(enterOperation = operation, exitOperation = { })

    fun <T: AstNode> process(nodeClass: Class<T>, operation: (T) -> Unit) = process(nodeClass, enterOperation = operation, exitOperation = { })

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
