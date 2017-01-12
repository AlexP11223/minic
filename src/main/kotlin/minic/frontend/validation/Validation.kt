package minic.frontend.validation

import minic.frontend.ast.*

fun Program.validate() : List<Error> {
    val errors = mutableListOf<Error>()

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
        when(it) {
            is BreakStatement -> errors.add(Error("Unexpected break statement", it.position!!.start)) // breaks not inside while are not allowed
            is WhileStatement -> return false // don't go into while children nodes, all breaks here are fine
        }
        return true
    })

    return errors.sortedBy { it.position }
}
