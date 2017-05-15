package minic.backend.codegen.jvm.info

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

internal class BytecodeDecompiler(val classBytes: ByteArray) {

    fun methodText(methodName: String, showFrames: Boolean): String {
        val result = StringBuilder()

        val reader = ClassReader(classBytes)
        val classNode = ClassNode()
        reader.accept(classNode, 0)

        val method = classNode.methods.first { (it as MethodNode).name == methodName } as MethodNode

        val printer = MethodTextifier(showFrames)
        val mp = TraceMethodVisitor(printer)

        for (insn in method.instructions) {
            (insn as AbstractInsnNode).accept(mp)
            val sw = StringWriter()
            printer.print(PrintWriter(sw))
            printer.getText().clear()
            result.append(sw)
        }

        return result.toString()
    }
}
