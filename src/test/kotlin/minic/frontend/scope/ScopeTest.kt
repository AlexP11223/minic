package minic.frontend.scope

import minic.frontend.type.*
import org.junit.Test
import kotlin.test.*

class ScopeTest {

    @Test
    fun canDefineResolveSymbol() {
        val scope = GlobalScope()

        val xSymbol = VariableSymbol("x", IntType)

        scope.define(xSymbol)

        assertEquals(xSymbol, scope.resolve("x"))
    }

    @Test
    fun canResolveFromParentScope() {
        val globalScope = GlobalScope()
        val localScope1 = LocalScope(globalScope)
        val localScope2 = LocalScope(localScope1)

        val xSymbol = VariableSymbol("x", IntType)
        val ySymbol = VariableSymbol("y", IntType)
        val zSymbol = VariableSymbol("z", IntType)

        globalScope.define(xSymbol)
        localScope1.define(ySymbol)
        localScope2.define(zSymbol)

        assertEquals(xSymbol, localScope1.resolve("x"))
        assertEquals(xSymbol, localScope2.resolve("x"))
        assertEquals(ySymbol, localScope2.resolve("y"))
        assertEquals(zSymbol, localScope2.resolve("z"))
    }

    @Test
    fun returnsNullWhenSymbolNotFound() {
        val globalScope = GlobalScope()
        val localScope1 = LocalScope(globalScope)
        val localScope2 = LocalScope(localScope1)

        val xSymbol = VariableSymbol("x", IntType)
        val ySymbol = VariableSymbol("y", IntType)
        val zSymbol = VariableSymbol("z", IntType)

        globalScope.define(xSymbol)
        localScope1.define(ySymbol)
        localScope2.define(zSymbol)

        assertEquals(null, localScope1.resolve("a"))
        assertEquals(null, globalScope.resolve("y"))
        assertEquals(null, localScope1.resolve("z"))
    }
}