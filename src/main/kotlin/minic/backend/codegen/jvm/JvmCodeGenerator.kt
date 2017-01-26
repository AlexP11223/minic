package minic.backend.codegen.jvm

import minic.frontend.ast.*
import minic.frontend.scope.Scope
import minic.frontend.scope.processWithSymbols
import minic.frontend.type.BoolType
import minic.frontend.type.DoubleType
import minic.frontend.type.IntType
import minic.frontend.type.StringType
import minic.frontend.type.Type
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.util.CheckClassAdapter

class JvmCodeGenerator(val ast: Program, val className: String = "MinicMain", val diagnosticChecks: Boolean = false) {
    private var nextVarIndex = 0
    private val varIndexMap = mutableMapOf<String, Int>()

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
        cv.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null)

        writeInitMethod(cv)

        writeProgramExecutionMethod(cv)

        writeMainMethod(cv)

        cv.visitEnd()
        return classWriter.toByteArray()
    }

    // constructor without parameters, also calls Java Object constructor
    private fun writeInitMethod(cv: ClassVisitor) {
        writeMethod(ACC_PUBLIC, "<init>", "()V", cv) { mv ->
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            mv.visitInsn(RETURN)
        }
    }

    // non-static execute() method with Mini-C program code
    private fun writeProgramExecutionMethod(cv: ClassVisitor) {
        writeMethod(ACC_PUBLIC, "execute", "()V", cv) { mv ->
            writeProgramCode(mv)

            mv.visitInsn(RETURN)
        }
    }

    // static main method. Creates class instance and calls execute() method
    private fun writeMainMethod(cv: ClassVisitor) {
        writeMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", cv) { mv ->
            mv.visitTypeInsn(NEW, className)
            mv.visitInsn(DUP)
            mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "()V", false)
            mv.visitMethodInsn(INVOKEVIRTUAL, className, "execute", "()V", false)
        }
    }

    private fun writeMethod(access: Int, name: String, desc: String, cv: ClassVisitor, writeCode: (mv: MethodVisitor) -> Unit) {
        val mv = cv.visitMethod(access, name, desc, null, null)
        mv.visitCode()

        writeCode(mv)

        mv.visitInsn(RETURN)

        // computed automatically because COMPUTE_MAXS option used.
        // but looks like CheckClassAdapter doesn't work without valid values
        mv.visitMaxs(100, 100)

        mv.visitEnd()
    }

    private fun writeProgramCode(mv: MethodVisitor) {
        ast.processWithSymbols(Statement::class.java, exitOperation = { statement, scope ->
            when (statement) {
                is VariableDeclaration -> {
                    // TODO: should be optimized to reuse indexes when left scope
                    val symbol = scope.resolve(statement.variableName)!!
                    val index = nextVarIndex

                    varIndexMap[statement.variableName] = index

                    nextVarIndex += when (symbol.type) {
                        IntType, BoolType, StringType -> 1
                        DoubleType -> 2
                        else -> throw UnsupportedOperationException("Declaration of ${symbol.type.name}")
                    }

                    statement.value.pushAs(symbol.type, scope, mv)

                    when (symbol.type) {
                        IntType, BoolType -> mv.visitVarInsn(ISTORE, index)
                        DoubleType -> mv.visitVarInsn(DSTORE, index)
                        StringType -> mv.visitVarInsn(ASTORE, index)
                        else -> throw UnsupportedOperationException("Declaration of ${symbol.type.name}")
                    }
                }
                is Assignment -> {
                    val symbol = scope.resolve(statement.variableName)!!
                    val index = varIndexMap[statement.variableName]!!

                    statement.value.pushAs(symbol.type, scope, mv)

                    when (symbol.type) {
                        IntType, BoolType -> mv.visitVarInsn(ISTORE, index)
                        DoubleType -> mv.visitVarInsn(DSTORE, index)
                        StringType -> mv.visitVarInsn(ASTORE, index)
                        else -> throw UnsupportedOperationException(symbol.type.name)
                    }
                }
                is PrintStatement -> {
                    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                    statement.value.push(scope, mv)
                    val printFunc = if (statement.newline) "println" else "print"
                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", printFunc, "(Ljava/lang/String;)V", false)

                }
                is StatementsBlock -> { /* no need to do anything, process() already visits all children */ }
                else -> throw UnsupportedOperationException(statement.javaClass.canonicalName)
            }
        })
    }

    /**
     * Pushes expression to stack
     */
    private fun Expression.push(scope: Scope, mv: MethodVisitor) {
        when (this) {
            is IntLiteral -> mv.visitLdcInsn(value)
            is FloatLiteral -> mv.visitLdcInsn(value)
            is StringLiteral -> mv.visitLdcInsn(value)
            is BooleanLiteral -> mv.visitLdcInsn(if (value) 1 else 0)
            is VariableReference -> {
                val symbol = scope.resolve(variableName)!!
                val index = varIndexMap[variableName]!!
                when (symbol.type) {
                    IntType, BoolType -> mv.visitVarInsn(ILOAD, index)
                    DoubleType -> mv.visitVarInsn(DLOAD, index)
                    StringType -> mv.visitVarInsn(ALOAD, index)
                    else -> throw UnsupportedOperationException(symbol.type.name)
                }
            }
            is ToString -> {
                value.push(scope, mv)
                val argType = value.type(scope)
                when (argType) {
                    IntType -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false)
                    DoubleType -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "toString", "(D)Ljava/lang/String;", false)
                    BoolType -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "toString", "(Z)Ljava/lang/String;", false)
                    StringType -> { /* do nothing, already have string on the stack */ }
                    else -> throw UnsupportedOperationException(argType.javaClass.canonicalName)
                }
            }
            is UnaryMinusExpression -> {
                value.push(scope, mv)
                val valueType = value.type(scope)
                when (valueType) {
                    IntType -> mv.visitInsn(INEG)
                    DoubleType -> mv.visitInsn(DNEG)
                    else -> throw UnsupportedOperationException(valueType.javaClass.canonicalName)
                }
            }
            is NotExpression -> {
                expr.pushAs(BoolType, scope, mv)
                val lblBeforeFalse = Label()
                mv.visitJumpInsn(IFNE, lblBeforeFalse)
                mv.visitInsn(ICONST_1)
                val lblEnd = Label()
                mv.visitJumpInsn(GOTO, lblEnd)
                mv.visitLabel(lblBeforeFalse)
                mv.visitInsn(ICONST_0)
                mv.visitLabel(lblEnd)
            }
            is BinaryExpression -> {
                // promote types if needed (such as int to double)
                val leftType = left.type(scope).promoteTo(right.type(scope))
                val rightType = left.type(scope).promoteTo(leftType)
                if (leftType != rightType)
                    throw UnsupportedOperationException("${leftType.name} and ${rightType.name}")

                when (this) {
                    is AdditionExpression -> {
                        if (leftType == StringType) { // assuming that it was already validated and string concatenation allowed only if both strings
                            mv.visitTypeInsn(NEW, "java/lang/StringBuilder")
                            mv.visitInsn(DUP)
                            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                            left.push(scope, mv)
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                            right.push(scope, mv)
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                        } else {
                            left.pushAs(leftType, scope, mv)
                            right.pushAs(rightType, scope, mv)
                            val exprType = leftType
                            when (exprType) {
                                IntType -> mv.visitInsn(IADD)
                                DoubleType -> mv.visitInsn(DADD)
                                else -> throw UnsupportedOperationException("${this.javaClass.canonicalName} and ${exprType.name}")
                            }
                        }
                    }
                    is SubtractionExpression,
                    is MultiplicationExpression,
                    is DivisionExpression,
                    is ModExpression -> {
                        left.pushAs(leftType, scope, mv)
                        right.pushAs(rightType, scope, mv)
                        val exprType = leftType
                        when (this) {
                            is SubtractionExpression -> {
                                when (exprType) {
                                    IntType -> mv.visitInsn(ISUB)
                                    DoubleType -> mv.visitInsn(DSUB)
                                }
                            }
                            is MultiplicationExpression -> {
                                when (exprType) {
                                    IntType -> mv.visitInsn(IMUL)
                                    DoubleType -> mv.visitInsn(DMUL)
                                }
                            }
                            is DivisionExpression -> {
                                when (exprType) {
                                    IntType -> mv.visitInsn(IDIV)
                                    DoubleType -> mv.visitInsn(DDIV)
                                }
                            }
                            is ModExpression -> {
                                when (exprType) {
                                    IntType -> mv.visitInsn(IREM)
                                    DoubleType -> mv.visitInsn(DREM)
                                }
                            }
                        }
                    }
                    // TODO: implement logical and/or instead of bitwise
                    is AndExpression -> {
                        left.pushAs(BoolType, scope, mv)
                        right.pushAs(BoolType, scope, mv)
                        mv.visitInsn(IAND)
                    }
                    is OrExpression -> {
                        left.pushAs(BoolType, scope, mv)
                        right.pushAs(BoolType, scope, mv)
                        mv.visitInsn(IOR)
                    }
                    else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
                }
            }
            else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
        }
    }

    /**
     * Pushes expression to stack and converts it to resultType if needed and possible
     * @throws UnsupportedOperationException if cannot convert
     */
    private fun Expression.pushAs(resultType: Type, scope: Scope, mv: MethodVisitor) {
        this.push(scope, mv)
        val currentType = this.type(scope)
        if (currentType != resultType) {
            if (currentType == IntType && resultType == DoubleType) {
                mv.visitInsn(I2D)
            } else {
                throw UnsupportedOperationException("Cannot convert ${currentType.name} to ${resultType.name}")
            }
        }
    }
}