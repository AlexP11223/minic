package minic.backend.codegen.jvm

import minic.backend.ExecutionRuntimeException
import minic.backend.codegen.jvm.info.BytecodeTextifierVisitor
import minic.frontend.ast.*
import minic.frontend.scope.Scope
import minic.frontend.scope.Symbol
import minic.frontend.scope.processWithSymbolsUntil
import minic.frontend.type.BoolType
import minic.frontend.type.DoubleType
import minic.frontend.type.IntType
import minic.frontend.type.StringType
import minic.frontend.type.Type
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.util.CheckClassAdapter
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.nio.charset.StandardCharsets


/**
 * @param className Name of produced JVM class. Should be the same as name of .class file,
 *                  does not matter if not saved to a file to be executed by java.
 * @param diagnosticChecks Enables additional checks during code generation (bytecode correctness).
 * @param debugInfo Adds additional information, such as source code line numbers in bytecode
 */
internal class JvmCodeGenerator(val ast: Program, val className: String = "MinicMain",
                                val diagnosticChecks: Boolean = false, val debugInfo: Boolean = false) {
    private var nextVarIndex = 1
    private val varIndexMap = mutableMapOf<String, Int>()

    private var lastInputFuncType: InputFunction? = null

    private data class LoopData(val endLabel: Label)

    private val loopStack = Stack<LoopData>()

    private var lastLineNumber: Int = -1

    /**
     * JVM bytecode. Can be saved to a .class file, executed, etc.
     */
    val bytes: ByteArray

    val bytecodeText: String

    init {
        // TODO: refactor to generate text only if needed?
        val bytecodeTextBaos = ByteArrayOutputStream()

        bytes = compile(PrintWriter(bytecodeTextBaos))

        bytecodeText = String(bytecodeTextBaos.toByteArray(), StandardCharsets.UTF_8)
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
        try {
            programClass.getMethod("execute").invoke(inst)
        } catch (ex: InvocationTargetException) {
            if (ex.targetException != null) {
                throw ExecutionRuntimeException(ex.targetException.message, ex.targetException)
            }
            throw ExecutionRuntimeException(ex.message, ex)
        } catch (ex: Exception) {
            throw ExecutionRuntimeException(ex.message, ex)
        }
    }

    private fun compile(bytecodeTextWriter: PrintWriter? = null): ByteArray {
        assert(nextVarIndex == 1, { "compile() called more than once" })

        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS)
        val cv = if (diagnosticChecks) CheckClassAdapter(classWriter, true) else classWriter
        cv.visit(V1_8, ACC_PUBLIC, className, null, "java/lang/Object", null)

        writeInitMethod(cv)

        writeProgramExecutionMethod(if (bytecodeTextWriter == null) cv else BytecodeTextifierVisitor(cv, bytecodeTextWriter))

        writeMainMethod(cv)

        cv.visitEnd()
        return classWriter.toByteArray()
    }

    private fun hasReadOperations(): Boolean {
        var result = false
        ast.process {
            if (it is InputFunction) {
                result = true
            }
        }
        return result
    }

    // constructor without parameters, calls Java Object constructor and creates Scanner if needed
    private fun writeInitMethod(cv: ClassVisitor) {
        writeMethod(ACC_PUBLIC, "<init>", "()V", cv) { mv ->
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

            if (hasReadOperations()) {
                cv.visitField(ACC_PRIVATE + ACC_FINAL, "scanner", "Ljava/util/Scanner;", null, null).visitEnd()
                mv.visitVarInsn(ALOAD, 0)
                mv.visitTypeInsn(NEW, "java/util/Scanner")
                mv.visitInsn(DUP)
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;")
                mv.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false)
                mv.visitFieldInsn(GETSTATIC, "java/util/Locale", "US", "Ljava/util/Locale;")
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "useLocale", "(Ljava/util/Locale;)Ljava/util/Scanner;", false)
                mv.visitFieldInsn(PUTFIELD, className, "scanner", "Ljava/util/Scanner;")
            }

            mv.visitInsn(RETURN)
        }
    }

    // non-static execute() method with Mini-C program code
    private fun writeProgramExecutionMethod(cv: ClassVisitor) {
        writeMethod(ACC_PUBLIC, "execute", "()V", cv) { mv ->
            writeProgramCode(ast, mv)
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


    private fun writeProgramCode(ast: Program, mv: MethodVisitor) {
        ast.processWithSymbolsUntil(Statement::class.java, enterOperation = { statement, scope ->
            writeStatementCode(statement, scope, mv)
        }, exitOperation = { statement, scope ->
            handleStatementEnd(statement, scope)
        })
    }

    private fun writeProgramCode(node: Statement, scope: Scope, mv: MethodVisitor) {
        node.processWithSymbolsUntil(scope, Statement::class.java, enterOperation = { statement, scope ->
            writeStatementCode(statement, scope, mv)
        }, exitOperation = { statement, scope ->
            handleStatementEnd(statement, scope)
        })
    }

    /**
     * @return false when children statements should not be visited
     */
    private fun writeStatementCode(statement: Statement, scope: Scope, mv: MethodVisitor): Boolean {
        if (debugInfo) {
            val line = statement.position!!.start.line
            if (lastLineNumber < line) {
                lastLineNumber = line
                val label = Label()
                mv.visitLabel(label)
                mv.visitLineNumber(line, label)
            }
        }

        when (statement) {
            is VariableDeclaration -> {
                val symbol = scope.resolve(statement.variableName)!!
                val index = nextVarIndex

                varIndexMap[statement.variableName] = index

                nextVarIndex += symbol.jvmVariableSize()

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
            is IfStatement -> {
                statement.expr.pushAs(BoolType, scope, mv)

                val lblElse = Label()
                mv.visitJumpInsn(IFEQ, lblElse)

                writeProgramCode(statement.ifBody, scope, mv)

                if (statement.elseBody == null) {
                    mv.visitLabel(lblElse)
                } else {
                    val lblEnd = Label()
                    mv.visitJumpInsn(GOTO, lblEnd)
                    mv.visitLabel(lblElse)

                    writeProgramCode(statement.elseBody, scope, mv)

                    mv.visitLabel(lblEnd)
                }

                return false // don't visit children statements, already processed
            }
            is WhileStatement -> {
                val lblCondition = Label()
                mv.visitLabel(lblCondition)

                statement.expr.pushAs(BoolType, scope, mv)

                val lblEnd = Label()
                mv.visitJumpInsn(IFEQ, lblEnd)

                loopStack.push(LoopData(lblEnd))

                writeProgramCode(statement.statement, scope, mv)

                mv.visitJumpInsn(GOTO, lblCondition)
                mv.visitLabel(lblEnd)

                return false // don't visit children statements, already processed
            }
            is BreakStatement -> {
                val currentLoop = loopStack.peek()
                mv.visitJumpInsn(GOTO, currentLoop.endLabel)
            }
            is ExitStatement -> {
                mv.visitInsn(ICONST_0)
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "exit", "(I)V", false)
            }
            is StatementsBlock -> { /* no need to do anything, process() already visits all children */ }
            is EmptyStatement -> { /* no need to generate anything for empty statements (";") */ }
            else -> throw UnsupportedOperationException(statement.javaClass.canonicalName)
        }
        return true
    }

    private fun handleStatementEnd(statement: Statement, scope: Scope) {
        when (statement) {
            is WhileStatement -> {
                loopStack.pop()
            }
            is StatementsBlock -> {
                nextVarIndex -= scope.symbols().map { it.jvmVariableSize() }.sum()

                assert(nextVarIndex > 0)
            }
        }
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
            is InputFunction -> {
                mv.visitVarInsn(ALOAD, 0)
                mv.visitFieldInsn(GETFIELD, className, "scanner", "Ljava/util/Scanner;")
                when (this) {
                    is ReadInt -> mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextInt", "()I", false)
                    is ReadDouble -> mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextDouble", "()D", false)
                    is ReadLine -> {
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextLine", "()Ljava/lang/String;", false)

                        // skip newline that was not consumed by nextInt/nextDouble to avoid confusing behavior
                        if (lastInputFuncType != null && lastInputFuncType !is ReadLine && this is ReadLine) {
                            mv.visitInsn(DUP)
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "isEmpty", "()Z", false)
                            val lblEnd = Label()
                            mv.visitJumpInsn(IFEQ, lblEnd)
                            mv.visitInsn(POP)
                            mv.visitVarInsn(ALOAD, 0)
                            mv.visitFieldInsn(GETFIELD, className, "scanner", "Ljava/util/Scanner;")
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "nextLine", "()Ljava/lang/String;", false)
                            mv.visitLabel(lblEnd)
                        }
                    }
                    else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
                }
                lastInputFuncType = this
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
                pushNot(mv)
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
                    is AndExpression -> {
                        left.pushAs(BoolType, scope, mv)
                        val lblFalse = Label()
                        mv.visitJumpInsn(IFEQ, lblFalse)

                        right.pushAs(BoolType, scope, mv)
                        mv.visitJumpInsn(IFEQ, lblFalse)

                        mv.visitLdcInsn(1)
                        val lblEnd = Label()
                        mv.visitJumpInsn(GOTO, lblEnd)

                        mv.visitLabel(lblFalse)
                        mv.visitLdcInsn(0)
                        mv.visitLabel(lblEnd)
                    }
                    is OrExpression -> {
                        left.pushAs(BoolType, scope, mv)
                        val lblTrue = Label()
                        mv.visitJumpInsn(IFNE, lblTrue)

                        right.pushAs(BoolType, scope, mv)
                        val lblFalse = Label()
                        mv.visitJumpInsn(IFEQ, lblFalse)

                        mv.visitLabel(lblTrue)
                        mv.visitLdcInsn(1)
                        val lblEnd = Label()
                        mv.visitJumpInsn(GOTO, lblEnd)

                        mv.visitLabel(lblFalse)
                        mv.visitLdcInsn(0)
                        mv.visitLabel(lblEnd)
                    }
                    is EqualityExpression -> {
                        left.pushAs(leftType, scope, mv)
                        right.pushAs(rightType, scope, mv)
                        val type = leftType
                        when (type) {
                            StringType -> {
                                mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
                                if (this is NotEqualExpression) {
                                    // !equals(...)
                                    pushNot(mv)
                                }
                            }
                            else -> {
                                if (type == DoubleType) {
                                    mv.visitInsn(DCMPL)
                                }
                                val comparisonOpcode = when (type) {
                                    IntType, BoolType -> when (this) {
                                        is EqualExpression -> IF_ICMPNE
                                        is NotEqualExpression -> IF_ICMPEQ
                                        else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
                                    }
                                    DoubleType ->  when (this) {
                                        is EqualExpression -> IFNE
                                        is NotEqualExpression -> IFEQ
                                        else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
                                    }
                                    else -> throw UnsupportedOperationException(type.name)
                                }
                                pushComparison(comparisonOpcode, mv)
                            }
                        }
                    }
                    is RelationalExpression -> {
                        left.pushAs(leftType, scope, mv)
                        right.pushAs(rightType, scope, mv)
                        val type = leftType
                        if (type == DoubleType) {
                            when (this) {
                                is LessExpression, is LessOrEqualExpression -> mv.visitInsn(DCMPG)
                                is GreaterExpression, is GreaterOrEqualExpression -> mv.visitInsn(DCMPL)
                            }
                        }
                        val comparisonOpcode = when (type) {
                            IntType, BoolType -> when (this) {
                                is LessExpression -> IF_ICMPGE
                                is GreaterExpression -> IF_ICMPLE
                                is LessOrEqualExpression -> IF_ICMPGT
                                is GreaterOrEqualExpression -> IF_ICMPLT
                                else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
                            }
                            DoubleType ->  when (this) {
                                is LessExpression -> IFGE
                                is GreaterExpression -> IFLE
                                is LessOrEqualExpression -> IFGT
                                is GreaterOrEqualExpression -> IFLT
                                else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
                            }
                            else -> throw UnsupportedOperationException(type.name)
                        }
                        pushComparison(comparisonOpcode, mv)
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

    private fun pushComparison(opcode: Int, mv: MethodVisitor) {
        val lblBeforeFalse = Label()
        mv.visitJumpInsn(opcode, lblBeforeFalse)
        mv.visitInsn(ICONST_1)
        val lblEnd = Label()
        mv.visitJumpInsn(GOTO, lblEnd)
        mv.visitLabel(lblBeforeFalse)
        mv.visitInsn(ICONST_0)
        mv.visitLabel(lblEnd)
    }

    private fun pushNot(mv: MethodVisitor) {
        pushComparison(IFNE, mv)
    }

    private fun Symbol.jvmVariableSize() = when (type) {
        IntType, BoolType, StringType -> 1
        DoubleType -> 2
        else -> throw UnsupportedOperationException("Declaration of ${type.name}")
    }
}