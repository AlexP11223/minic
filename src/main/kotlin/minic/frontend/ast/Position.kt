package minic.frontend.ast

data class Point(val line: Int, val column: Int) : Comparable<Point> {

    override fun compareTo(other: Point): Int = when {
        line != other.line -> line - other.line
        else -> column - other.column
    }

    override fun toString() = "Line $line, column $column"
}

data class Position(val start: Point, val end: Point)
