package minic.frontend.type

interface Type {
    val name: String
        get() = javaClass.simpleName.removeSuffix("Type").toLowerCase()
}

object IntType : Type
object DoubleType : Type
object StringType : Type
object BoolType : Type
