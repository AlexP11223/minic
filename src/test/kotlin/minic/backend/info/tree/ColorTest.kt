package minic.backend.info.tree

import org.junit.Test
import java.awt.Color
import kotlin.test.assertEquals

class ColorTest {
    @Test
    fun convertToRgbString() {
        assertEquals("ff0000", Color.RED.toRgbString())
        assertEquals("00ff00", Color.GREEN.toRgbString())
        assertEquals("0000ff", Color.BLUE.toRgbString())
        assertEquals("010203", Color(1, 2, 3).toRgbString())
    }
}
