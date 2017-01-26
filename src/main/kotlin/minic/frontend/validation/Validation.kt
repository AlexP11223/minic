package minic.frontend.validation

import minic.frontend.IllegalExpressionException
import minic.frontend.UndefinedSymbolException
import minic.frontend.ast.*
import minic.frontend.scope.*
import minic.frontend.type.*

/**
 * Returns list of errors (only semantic), or empty list if there are no errors
 */
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

    // check expression types
    // expressions can be only inside of some statements, so we need to check only them
    // for incompatible types inside expressions type() throws exceptions
    this.processWithSymbols(Statement::class.java, enterOperation = { node, scope ->
        try {
            when (node) {
                is VariableDeclaration -> {
                    val exprType = node.value.type(scope)
                    if (exprType != node.variableType && !exprType.canPromoteTo(node.variableType.type)) {
                        errors.add(Error("Cannot assign expression of type '${exprType.name}' to a variable of type '${node.variableType.name}'", node.position!!.start))
                    }
                }
                is Assignment -> {
                    val variable = scope.resolve(node.variableName)
                    if (variable != null) {
                        val exprType = node.value.type(scope)
                        if (exprType != variable.type && !exprType.canPromoteTo(variable.type)) {
                            errors.add(Error("Cannot assign expression of type '${exprType.name}' to a variable of type '${variable.type.name}'", node.position!!.start))
                        }
                    }
                }
                is IfStatement -> {
                    val exprType = node.expr.type(scope)
                    if (exprType != BoolType) {
                        errors.add(Error("Expression must be '${BoolType.name}', got '${exprType.name}'", node.expr.position!!.start))
                    }
                }
                is WhileStatement -> {
                    val exprType = node.expr.type(scope)
                    if (exprType != BoolType) {
                        errors.add(Error("Expression must be '${BoolType.name}', got '${exprType.name}'", node.expr.position!!.start))
                    }
                }
                is PrintStatement -> {
                    val exprType = node.value.type(scope)
                    if (exprType != StringType) {
                        errors.add(Error("Expression must be '${StringType.name}', got '${exprType.name}'", node.value.position!!.start))
                    }
                }
            }
        }
        catch (e: UndefinedSymbolException) {
            // ignore, should be already detected
        }
        catch (e: IllegalExpressionException) {
            errors.add(Error(e.message ?: "Illegal expression", e.node.position!!.start))
        }
    })

    return errors.distinct().sortedBy { it.position }
}
