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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue

internal class AstGraphvizRenderer(val ast: Program) {

    companion object {

        private val graphvizThread by lazy {
            val t = object : Thread() {
                private val commands = LinkedBlockingQueue<Pair<() -> Unit, CountDownLatch>>()

                override fun run() {
                    while (true) {
                        val (cmd, latch) = commands.take()
                        try {
                            cmd()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                fun enqueueCommand(cmd: () -> Unit, latch: CountDownLatch) {
                    commands.put(cmd to latch)
                }

                @Suppress("unused") // Kotlin/Idea bug? reports unused
                fun <T> processCommand(cmd: () -> T): T {
                    var error: Throwable? = null
                    var result: T? = null
                    val wrappedCmd = {
                        try {
                            result = cmd()
                        } catch (ex: Throwable) {
                            error = ex
                        }
                    }

                    val latch = CountDownLatch(1)
                    enqueueCommand(wrappedCmd, latch)
                    latch.await()

                    if (error != null) {
                        throw Exception(error)
                    }
                    return result!!
                }
            }
            t.isDaemon = true
            t.setUncaughtExceptionHandler { _, e ->
                System.err.println(e)
                System.exit(1)
            }
            t.start()
            t
        }
    }

    private var nodeCounter = 0

    fun render(): BufferedImage {
        return graphvizThread.processCommand {
            Graphviz.fromGraph(createGraph()).render(Format.PNG).toImage()
        }
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
