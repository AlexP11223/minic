package minic.backend.codegen.jvm

class DynamicClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    fun define(className: String, bytecode: ByteArray): Class<*> {
        return super.defineClass(className, bytecode, 0, bytecode.size)
    }
}
