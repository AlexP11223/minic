package minic.backend.codegen.jvm

import minic.frontend.ast.*
import minic.frontend.scope.Scope
import minic.frontend.scope.processWithSymbols
import minic.frontend.type.BoolType
import minic.frontend.type.DoubleType
import minic.frontend.type.IntType
import minic.frontend.type.StringType
import org.objectweb.asm.*
import org.objectweb.asm.util.CheckClassAdapter

class JvmCodeGenerator(val ast: Program, val className: String = "MinicMain", val diagnosticChecks: Boolean = false) {
    /**
     * JVM bytecode. Can be saved to a .class file, executed, etc.
     */
    val bytes: ByteArray

    init {
        bytes = compile()
    }

    /**
     * Executes program in current thread
     */
    fun execute() {
        val loader = DynamicClassLoader(Thread.currentThread().contextClassLoader)
        val programClass = loader.define(className, bytes)
        val inst = programClass.newInstance()
        // better approach would be to implement interface and cast to it
        // but it is more complicated to generate so for now just calling it using reflection
        programClass.getMethod("execute").invoke(inst)
    }

    private fun compile(): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS)
        val cv = if (diagnosticChecks) CheckClassAdapter(classWriter, true) else classWriter
        cv.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null)

        writeInitMethod(cv)

        writeProgramExecutionMethod(cv)

        writeMainMethod(cv)

        cv.visitEnd()
        return classWriter.toByteArray()
    }

    // constructor without parameters, also calls Java Object constructor
    private fun writeInitMethod(cv: ClassVisitor) {
        writeMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", cv) { mv ->
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            mv.visitInsn(Opcodes.RETURN)
        }
    }

    // non-static execute() method with Mini-C program code
    private fun writeProgramExecutionMethod(cv: ClassVisitor) {
        writeMethod(Opcodes.ACC_PUBLIC, "execute", "()V", cv) { mv ->
            writeProgramCode(mv)

            mv.visitInsn(Opcodes.RETURN)
        }
    }

    // static main method. Creates class instance and calls execute() method
    private fun writeMainMethod(cv: ClassVisitor) {
        writeMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", cv) { mv ->
            mv.visitTypeInsn(Opcodes.NEW, className)
            mv.visitInsn(Opcodes.DUP)
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", "()V", false)
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "execute", "()V", false)
        }
    }

    private fun writeMethod(access: Int, name: String, desc: String, cv: ClassVisitor, writeCode: (mv: MethodVisitor) -> Unit) {
        val mv = cv.visitMethod(access, name, desc, null, null)
        mv.visitCode()

        writeCode(mv)

        mv.visitInsn(Opcodes.RETURN)

        // computed automatically because COMPUTE_MAXS option used.
        // but looks like CheckClassAdapter doesn't work without valid values
        mv.visitMaxs(100, 100)

        mv.visitEnd()
    }

    private fun writeProgramCode(mv: MethodVisitor) {
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