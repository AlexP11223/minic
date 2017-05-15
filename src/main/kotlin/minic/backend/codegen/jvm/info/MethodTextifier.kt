package minic.backend.codegen.jvm.info

import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.Textifier

internal open class MethodTextifier(val showFrames: Boolean = true) : Textifier(Opcodes.ASM5) {
    init {
        tab2 = "  "
        ltab = ""
    }

    // not used when writing during codegen via BytecodeTextifierVisitor because there are no frames yet
    override fun visitFrame(type: Int, nLocal: Int, local: Array<out Any>?, nStack: Int, stack: Array<out Any>?) {
        if (showFrames) {
            super.visitFrame(type, nLocal, local, nStack, stack)
        } else {
            // output nothing
        }
    }
}
