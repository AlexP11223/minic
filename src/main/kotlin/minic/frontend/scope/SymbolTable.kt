package minic.frontend.scope

import minic.frontend.ast.*

// it would be more efficient to build symbol table once and store/reuse it somehow
// but it doesn't matter for simple languages like this, and it is not too computational-heavy operation anyway,
// so for now I decided to use this simple and clear approach

/**
 * Visits this node and all children nodes, builds scopes.
 * It adds new variables to the current scope when encountering declarations, pushes new scope when entering blocks
 * and pops the current scope when leaving blocks.
 * For each node calls beforeSymbolOperation before any scope manipulations (always, even when nothing will change),
 * enterOperation before visiting its children, and exitOperation after. All operations receive the node and scope as parameters.
 */
fun AstNode.processWithSymbols(scope: Scope,
                               beforeSymbolOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               enterOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               exitOperation: (AstNode, Scope) -> Unit = ::emptyOp)
        // calls processWithSymbolsUntil passing enterOperation that always returns true
        = processWithSymbolsUntil(scope, beforeSymbolOperation, { n, s -> enterOperation(n, s); true}, exitOperation)

/**
 * Starting point for [processWithSymbols], starts with empty global scope.
 */
fun Program.processWithSymbols(beforeSymbolOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               enterOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               exitOperation: (AstNode, Scope) -> Unit = ::emptyOp)
        = this.processWithSymbols(GlobalScope(), beforeSymbolOperation, enterOperation, exitOperation)

/**
 * The same as [processWithSymbols] but does not visit children if enterOperation returned false
 */
fun AstNode.processWithSymbolsUntil(scope: Scope,
                               beforeSymbolOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               enterOperation: (AstNode, Scope) -> Boolean = ::emptyOpBool,
                               exitOperation: (AstNode, Scope) -> Unit = ::emptyOp) {
    var currentScope = scope

    beforeSymbolOperation(this, currentScope)

    when (this) {
        is VariableDeclaration -> currentScope.define(VariableSymbol(variableName, variableType.type))
        is StatementsBlock -> currentScope = LocalScope(currentScope)
    }

    if (!enterOperation(this, currentScope))
        return

    children().forEach {
        it.processWithSymbolsUntil(currentScope, beforeSymbolOperation, enterOperation, exitOperation)
    }

    exitOperation(this, currentScope)
}

/**
 * Starting point for [processWithSymbolsUntil], starts with empty global scope.
 */
fun Program.processWithSymbolsUntil(beforeSymbolOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               enterOperation: (AstNode, Scope) -> Boolean = ::emptyOpBool,
                               exitOperation: (AstNode, Scope) -> Unit = ::emptyOp)
        = this.processWithSymbolsUntil(GlobalScope(), beforeSymbolOperation, enterOperation, exitOperation)

/**
 * The same as [processWithSymbolsUntil] but calls operations only if node is instance of the specified nodeClass
 */
fun <T: AstNode> AstNode.processWithSymbolsUntil(scope: Scope, nodeClass: Class<T>,
                                            beforeSymbolOperation: (T, Scope) -> Unit = ::emptyOp,
                                            enterOperation: (T, Scope) -> Boolean = ::emptyOpBool,
                                            exitOperation: (T, Scope) -> Unit = ::emptyOp) {
    processWithSymbolsUntil(scope, beforeSymbolOperation = { node, scope ->
        if (nodeClass.isInstance(node)) {
            beforeSymbolOperation(node as T, scope)
        }
    }, enterOperation = { node, scope ->
        if (nodeClass.isInstance(node)) {
            enterOperation(node as T, scope)
        } else {
            true
        }
    }, exitOperation = { node, scope ->
        if (nodeClass.isInstance(node)) {
            exitOperation(node as T, scope)
        }
    })
}

/**
 * Starting point for [processWithSymbols], starts with empty global scope.
 */
fun <T: AstNode> Program.processWithSymbolsUntil(nodeClass: Class<T>,
                                            beforeSymbolOperation: (T, Scope) -> Unit = ::emptyOp,
                                            enterOperation: (T, Scope) -> Boolean = ::emptyOpBool,
                                            exitOperation: (T, Scope) -> Unit = ::emptyOp)
        = this.processWithSymbolsUntil(GlobalScope(), nodeClass, beforeSymbolOperation, enterOperation, exitOperation)

/**
 * The same as [processWithSymbols] but calls operations only if node is instance of the specified nodeClass
 */
fun <T: AstNode> AstNode.processWithSymbols(scope: Scope, nodeClass: Class<T>,
                                            beforeSymbolOperation: (T, Scope) -> Unit = ::emptyOp,
                                            enterOperation: (T, Scope) -> Unit = ::emptyOp,
                                            exitOperation: (T, Scope) -> Unit = ::emptyOp) {
    processWithSymbols(scope, beforeSymbolOperation = { node, scope ->
        if (nodeClass.isInstance(node)) {
            beforeSymbolOperation(node as T, scope)
        }
    }, enterOperation = { node, scope ->
        if (nodeClass.isInstance(node)) {
            enterOperation(node as T, scope)
        }
    }, exitOperation = { node, scope ->
        if (nodeClass.isInstance(node)) {
            exitOperation(node as T, scope)
        }
    })
}

/**
 * Starting point for [processWithSymbols], starts with empty global scope.
 */
fun <T: AstNode> Program.processWithSymbols(nodeClass: Class<T>,
                                            beforeSymbolOperation: (T, Scope) -> Unit = ::emptyOp,
                                            enterOperation: (T, Scope) -> Unit = ::emptyOp,
                                            exitOperation: (T, Scope) -> Unit = ::emptyOp)
        = this.processWithSymbols(GlobalScope(), nodeClass, beforeSymbolOperation, enterOperation, exitOperation)

private fun emptyOp(node: AstNode, scope: Scope) { }
private fun emptyOpBool(node: AstNode, scope: Scope): Boolean { return true }
