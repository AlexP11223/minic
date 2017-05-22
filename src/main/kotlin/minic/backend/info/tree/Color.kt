package minic.backend.info.tree

import java.awt.Color

internal fun Color.toRgbString() = red.toString(16).padStart(2, '0') +
        green.toString(16).padStart(2, '0') +
        blue.toString(16).padStart(2, '0')
