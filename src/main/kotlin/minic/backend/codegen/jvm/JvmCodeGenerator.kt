package minic.backend.codegen.jvm

import minic.frontend.ast.*
import minic.frontend.scope.Scope
import minic.frontend.scope.processWithSymbols
import minic.frontend.type.BoolType
import minic.frontend.type.DoubleType
import minic.frontend.type.IntType
import minic.frontend.type.StringType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.CheckClassAdapter

class JvmCodeGenerator(val ast: Program, val className: String = "MiniCMain", val diagnosticChecks: Boolean = false) {
    val bytes: ByteArray

    init {
        bytes = compile()
    }

    private fun compile(): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS)
        val classVisitor = if (diagnosticChecks) CheckClassAdapter(cw, true) else cw
        classVisitor.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null)

        val mainMethodVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)
        mainMethodVisitor.visitCode()

        writeCode(mainMethodVisitor)

        mainMethodVisitor.visitInsn(Opcodes.RETURN)
        mainMethodVisitor.visitMaxs(100, 100) // computed automatically because COMPUTE_MAXS option used.
                                              // but looks like CheckClassAdapter doesn't work without valid values
        mainMethodVisitor.visitEnd()

        classVisitor.visitEnd()
        return cw.toByteArray()
    }

    private fun writeCode(mv: MethodVisitor) {
        ast.processWithSymbols(Statement::class.java) { statement, scope ->
            when (statement) {
                is VariableDeclaration -> {

                }
                is Assignment -> {

                }
                is PrintStatement -> {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                    statement.value.push(scope, mv)
                    val printFunc = if (statement.newline) "println" else "print"
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", printFunc, "(Ljava/lang/String;)V", false)

                }
                is StatementsBlock -> { /* no need to do anything, process() already visits all children */ }
                else -> throw UnsupportedOperationException(statement.javaClass.canonicalName)
            }
        }
    }

    private fun Expression.push(scope: Scope, mv: MethodVisitor) {
        when (this) {
            is IntLiteral -> mv.visitLdcInsn(value)
            is FloatLiteral -> mv.visitLdcInsn(value)
            is StringLiteral -> mv.visitLdcInsn(value)
            is BooleanLiteral -> mv.visitLdcInsn(if (value) 1 else 0)
            is ToString -> {
                value.push(scope, mv)
                val argType = value.type(scope)
                when (argType) {
                    IntType -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false)
                    DoubleType -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "toString", "(D)Ljava/lang/String;", false)
                    BoolType -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "toString", "(Z)Ljava/lang/String;", false)
                    StringType -> { /* do nothing, already have string on the stack */ }
                    else -> throw UnsupportedOperationException(argType.javaClass.canonicalName)
                }
            }
            else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
        }
    }
}