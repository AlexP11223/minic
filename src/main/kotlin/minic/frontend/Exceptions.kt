package minic.frontend

import minic.frontend.ast.AstNode

class UndefinedSymbolException(message: String, val node: AstNode) : RuntimeException(message)

class IllegalExpressionException(message: String, val node: AstNode) : RuntimeException(message)
