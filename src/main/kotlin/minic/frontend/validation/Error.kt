package minic.frontend.validation

import minic.frontend.ast.Point

data class Error(val message: String, val position: Point)
