package minic.backend.info.tree

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.graph
import guru.nidi.graphviz.model.Factory.node
import guru.nidi.graphviz.model.Graph
import guru.nidi.graphviz.model.Label
import guru.nidi.graphviz.model.Node
import minic.frontend.ast.AstNode
import minic.frontend.ast.Program
import java.awt.image.BufferedImage

internal class AstGraphvizRenderer(val ast: Program) {

    private var nodeCounter = 0

    fun render(): BufferedImage {
        return Graphviz.fromGraph(createGraph()).render(Format.PNG).toImage()
    }

    private fun createGraph(): Graph {
        nodeCounter = 0

        return graph().with(ast.toGraphNode())
    }

    private fun AstNode.toGraphNode() : Node {
        val nodeText = this.toText()
        return node(nodeCounter++.toString())
                .with(Label.of(nodeText))
                .link(*children().map { it.toGraphNode() }.toTypedArray())
    }
}
