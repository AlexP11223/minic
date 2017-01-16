package frontend.scope

import minic.frontend.ast.*
import minic.frontend.scope.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class ScopeTest {

    @Test
    fun canDefineResolveSymbol() {
        val scope = GlobalScope()

        val xSymbol = VariableSymbol("x", IntType())

        scope.define(xSymbol)

        assertEquals(xSymbol, scope.resolve("x"))
    }

    @Test
    fun canResolveFromParentScope() {
        val globalScope = GlobalScope()
        val localScope1 = LocalScope(globalScope)
        val localScope2 = LocalScope(localScope1)

        val xSymbol = VariableSymbol("x", IntType())
        val ySymbol = VariableSymbol("y", IntType())
        val zSymbol = VariableSymbol("z", IntType())

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

        val xSymbol = VariableSymbol("x", IntType())
        val ySymbol = VariableSymbol("y", IntType())
        val zSymbol = VariableSymbol("z", IntType())

        globalScope.define(xSymbol)
        localScope1.define(ySymbol)
        localScope2.define(zSymbol)

        assertEquals(null, localScope1.resolve("a"))
        assertEquals(null, globalScope.resolve("y"))
        assertEquals(null, localScope1.resolve("z"))
    }

    @Test
    fun equalsWorks() {
        val globalScope = GlobalScope()

        val scope1 = LocalScope(globalScope)
        scope1.define(VariableSymbol("i", IntType()))
        scope1.define(VariableSymbol("str", StringType()))

        val scope2 = LocalScope(globalScope)
        scope2.define(VariableSymbol("i", IntType()))
        scope2.define(VariableSymbol("str", StringType()))

        val scope3 = LocalScope(globalScope)
        scope3.define(VariableSymbol("i", IntType()))
        scope3.define(VariableSymbol("j", IntType()))

        val scope4 = LocalScope(scope1)
        scope4.define(VariableSymbol("i", IntType()))
        scope4.define(VariableSymbol("str", StringType()))

        assertFalse(scope1.equals(globalScope))

        assertEquals(scope1, scope2)
        assertNotEquals(scope1, scope3)

        assertNotEquals(scope1, scope4)
    }
}