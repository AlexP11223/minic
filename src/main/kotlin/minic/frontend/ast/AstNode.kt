package minic.frontend.ast

// base interface for all AST nodes
interface AstNode {
    val position: Position? // should not really be nullable, needed only to simplify some parsing tests

    fun children() : List<AstNode> = listOf() // no children by default

    /**
     * Visits this node and all children nodes.
     * Calls enterOperation on each node before visiting its children, and exitOperation after
     */
    fun process(enterOperation: (AstNode) -> Unit, exitOperation: (AstNode) -> Unit) {
        enterOperation(this)

        children().forEach {
            it.process(enterOperation, exitOperation)
        }

        exitOperation(this)
    }

    /**
     * The same as [process] but calls operations only if node is instance of the specified nodeClass
     * @param nodeClass any class with AstNode interface
     */
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

    /**
     * Overloaded method without exitOperation
     * @see process
     */
    fun process(operation: (AstNode) -> Unit) = process(enterOperation = operation, exitOperation = { })

    /**
     * Overloaded method without exitOperation
     */
    fun <T: AstNode> process(nodeClass: Class<T>, operation: (T) -> Unit) = process(nodeClass, enterOperation = operation, exitOperation = { })

    /**
     * The same as [process] but calls operation only if node is instance of any of the specified classes
     */
    fun process(nodeClasses: List<Class<out AstNode>>, operation: (AstNode) -> Unit) {
        process { node ->
            if (nodeClasses.any { it.isInstance(node) }) {
                operation(node)
            }
        }
    }

    /**
     * The same as [process] but does not visit children if operation returned false
     */
    fun processUntil(operation: (AstNode) -> Boolean) {
        if (operation(this)) {
            children().forEach {
                it.processUntil(operation)
            }
        }
    }
}
