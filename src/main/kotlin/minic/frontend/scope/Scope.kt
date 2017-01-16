package minic.frontend.scope

interface Scope {
    val parent: Scope?

    // adds symbol to the scope
    fun define(symbol: Symbol)

    // returns symbol if it exists in current or one of the parent scopes, or null
    fun resolve(name: String) : Symbol?

    // symbols of the scope
    fun symbols() : List<Symbol>

    // symbols of the scope including all parent scopes (starting from the deepest parent, that is global scope)
    fun allSymbols() : List<Symbol>
}

abstract class BaseScope() : Scope {
    private val symbols = mutableMapOf<String, Symbol>()

    override fun define(symbol: Symbol) {
        symbols[symbol.name] = symbol
    }

    override fun resolve(name: String): Symbol? {
        return symbols[name] ?: parent?.resolve(name)
    }

    override fun symbols(): List<Symbol> {
        return symbols.values.toList()
    }

    override fun allSymbols(): List<Symbol> {
        val list = mutableListOf<Symbol>()
        list += parent?.allSymbols() ?: emptyList()
        list += symbols()
        return list
    }

    override fun toString() = "symbols=$symbols"
}

class GlobalScope() : BaseScope() {
    override val parent: Scope?
        get() = null
}

class LocalScope(val parentScope: Scope) : BaseScope() {
    override val parent: Scope?
        get() = parentScope

    override fun toString() = "${super.toString()}, parent=${System.identityHashCode(parent)}"
}
