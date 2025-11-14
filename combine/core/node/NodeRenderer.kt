package top.fifthlight.combine.node

import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.paint.Canvas

fun interface NodeRenderer {
    fun Canvas.render(node: Placeable)

    companion object EmptyRenderer : NodeRenderer {
        override fun Canvas.render(node: Placeable) = Unit
    }
}