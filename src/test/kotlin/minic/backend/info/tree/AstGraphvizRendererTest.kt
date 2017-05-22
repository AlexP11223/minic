package minic.backend.info.tree

import minic.Compiler
import minic.frontend.ast.AstNode
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.Color
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
        val filePath = tmpFolder.root.absolutePath + "/ast.png"

        Compiler(code).drawAst(filePath)

        assertTrue(File(filePath).exists())
        assertTrue(File(filePath).length() > 1000)
    }

    @Test
    fun rendersImageWithPainter() {
        val code = """
int myVar = 42;
print(toString(myVar));
"""
        val compiler = Compiler(code)
        val ast = compiler.ast
        val img = compiler.drawAst(painter = object : TreePainter {
            override fun paintNode(node: AstNode): NodeStyle {
                if (node == ast.statements.first()) {
                    return NodeStyle(fillColor = Color.green)
                }
                return super.paintNode(node)
            }
        })

        assertTrue(img.width > 100)
        assertTrue(img.height > 100)
        assertEquals(TYPE_INT_ARGB, img.type)

        val pixels = mutableListOf<Color>()
        for (x in 0..img.width - 1) {
            for (y in 0..img.height - 1) {
                pixels.add(Color(img.getRGB(x, y)))
            }
        }

        assertTrue(pixels.any { it == Color.green }) }

    @Test
    fun canRunFromDifferentThreads() {
        for (i in 1..2) {
            var ex: Throwable? = null
            val t = thread(start = false) {
                rendersImage()
            }
            t.setUncaughtExceptionHandler { _, e ->
                ex = e
            }
            t.start()
            t.join()
            assertNull(ex, ex.toString())
        }
    }
}
