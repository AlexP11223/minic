package minic.backend.info.tree

import minic.Compiler
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AstGraphvizRendererTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    @Test
    fun rendersImage() {
        val code = """
int myVar = 42;
print(toString(myVar));
"""
        val img = Compiler(code).drawAst()

        assertTrue(img.width > 100)
        assertTrue(img.height > 100)
        assertEquals(TYPE_INT_ARGB, img.type)
    }

    @Test
    fun savesImage() {
        val code = """
int myVar = 42;
print(toString(myVar));
"""
        val filePath = tmpFolder.root.absolutePath + "/ast.png";

        Compiler(code).drawAst(filePath)

        assertTrue(File(filePath).exists())
        assertTrue(File(filePath).length() > 1000)
    }
}
