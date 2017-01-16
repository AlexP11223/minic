package minic.frontend.ast

import minic.frontend.ast.Point
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PointTest {

    @Test
    fun canComparePoints() {
        assertTrue(Point(42, 88) == Point(42, 88))
        assertTrue(Point(44, 88) != Point(42, 88))
        assertTrue(Point(42, 89) != Point(42, 88))

        assertTrue(Point(44, 88) > Point(42, 88))
        assertTrue(Point(42, 88) < Point(43, 88))

        assertTrue(Point(42, 89) > Point(42, 88))
        assertTrue(Point(42, 87) < Point(42, 88))

        assertFalse(Point(42, 88) < Point(42, 88))
        assertFalse(Point(42, 88) > Point(42, 88))
    }
}
