package minic.backend.info.tree

import minic.frontend.ast.AstNode
import java.awt.Color

interface TreePainter {
    fun paintNode(node: AstNode): NodeStyle = NodeStyle()
}

data class NodeStyle(val fillColor: Color? = null)
