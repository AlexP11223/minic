package minic.backend.codegen.jvm.info

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter

internal class BytecodeTextifierVisitor(cv: ClassVisitor, val printWriter: PrintWriter) : ClassVisitor(Opcodes.ASM5, cv) {

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
        val mv = cv.visitMethod(access, name, desc, signature, exceptions)

        val printer = object : MethodTextifier() {
            override fun visitMethodEnd() {
                this.print(printWriter)

                printWriter.flush()
            }
        }
        return TraceMethodVisitor(mv, printer)
    }
}
