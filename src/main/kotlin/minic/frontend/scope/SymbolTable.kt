package minic.frontend.scope

import minic.frontend.ast.*

// it would be more efficient to build symbol table once and store/reuse it somehow
// but it doesn't matter for simple languages like this, and it is not too computational-heavy operation anyway,
// so for now I decided to use this simple and clear approach

fun AstNode.processWithSymbols(scope: Scope,
                               enterOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               exitOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               beforeSymbolOperation: (AstNode, Scope) -> Unit = ::emptyOp) {
    var currentScope = scope

    beforeSymbolOperation(this, currentScope)

    when (this) {
        is VariableDeclaration -> currentScope.define(VariableSymbol(variableName, variableType.type))
        is StatementsBlock -> currentScope = LocalScope(currentScope)
    }

    enterOperation(this, currentScope)

    children().forEach {
        it.processWithSymbols(currentScope, enterOperation, exitOperation, beforeSymbolOperation)
    }

    exitOperation(this, currentScope)
}

fun Program.processWithSymbols(enterOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               exitOperation: (AstNode, Scope) -> Unit = ::emptyOp,
                               beforeSymbolOperation: (AstNode, Scope) -> Unit = ::emptyOp)
        = this.processWithSymbols(GlobalScope(), enterOperation, exitOperation, beforeSymbolOperation)

fun <T: AstNode> AstNode.processWithSymbols(scope: Scope, nodeClass: Class<T>,
                                            enterOperation: (T, Scope) -> Unit = ::emptyOp,
                                            exitOperation: (T, Scope) -> Unit = ::emptyOp,
                                            beforeSymbolOperation: (T, Scope) -> Unit = ::emptyOp) {
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

fun <T: AstNode> Program.processWithSymbols(nodeClass: Class<T>,
                                            enterOperation: (T, Scope) -> Unit = ::emptyOp,
                                            exitOperation: (T, Scope) -> Unit = ::emptyOp,
                                            beforeSymbolOperation: (T, Scope) -> Unit = ::emptyOp)
        = this.processWithSymbols(GlobalScope(), nodeClass, enterOperation, exitOperation, beforeSymbolOperation)

private fun emptyOp(node: AstNode, scope: Scope) { }