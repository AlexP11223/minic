package minic.frontend.validation

import minic.frontend.ast.*
import minic.frontend.scope.*

fun Program.validate() : List<Error> {
    val errors = mutableListOf<Error>()

    fun checkVariableDeclared(variableName: String, scope: Scope, node: AstNode) {
        if (scope.resolve(variableName) == null) {
            errors.add(Error("Variable '$variableName' is not declared", node.position!!.start))
        }
    }

    // check variable declarations
    this.processWithSymbols(beforeSymbolOperation = { node, scope ->
        when (node) {
            is VariableDeclaration -> {
                if (scope.resolve(node.variableName) != null) {
                    errors.add(Error("Variable '${node.variableName}' is already declared", node.position!!.start))
                }
            }
        }
    }, enterOperation = { node, scope ->
        when (node) {
            is VariableReference -> checkVariableDeclared(node.variableName, scope, node)
            is Assignment -> checkVariableDeclared(node.variableName, scope, node)
        }
    })

    // don't allow variable declarations in if/while without block
    this.process(listOf(WhileStatement::class.java, IfStatement::class.java)) {
        it.children().forEach {
            if (it is VariableDeclaration) {
                errors.add(Error("Variable declaration not allowed here", it.position!!.start))
            }
        }
    }

    // check break statements
    this.processUntil(fun(it: AstNode): Boolean {
        when (it) {
            is BreakStatement -> errors.add(Error("Unexpected break statement", it.position!!.start)) // breaks not inside while are not allowed
            is WhileStatement -> return false // don't go into while children nodes, all breaks here are fine
        }
        return true
    })

    return errors.sortedBy { it.position }
}
