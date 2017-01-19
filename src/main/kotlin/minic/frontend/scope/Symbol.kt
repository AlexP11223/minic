package minic.frontend.scope

import minic.frontend.type.Type

interface Symbol {
    val name: String
    val type: Type
}

data class VariableSymbol(override val name: String, override val type: Type) : Symbol
