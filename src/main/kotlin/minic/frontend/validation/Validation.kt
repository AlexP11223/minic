package minic.frontend.validation

import minic.frontend.ast.AstNode
import minic.frontend.ast.BreakStatement
import minic.frontend.ast.Program
import minic.frontend.ast.WhileStatement

fun Program.validate() : List<Error> {
    val errors = mutableListOf<Error>()

    // check break statements
    this.processUntil(fun(it: AstNode): Boolean {
        when(it) {
            is BreakStatement -> errors.add(Error("Unexpected break statement", it.position!!.start)) // breaks not inside while are not allowed
            is WhileStatement -> return false // don't go into while children nodes, all breaks here are fine
        }
        return true
    })

    return errors
}
