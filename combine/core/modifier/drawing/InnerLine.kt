package top.fifthlight.combine.modifier.drawing

import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Color
import top.fifthlight.data.IntOffset

fun Modifier.innerLine(color: Color) = then(InnerLineNode(color))

private data class InnerLineNode(
    val color: Color,
) : DrawModifierNode, Modifier.Node<InnerLineNode> {
    override fun Canvas.renderAfter(node: Placeable) {
        drawRect(IntOffset.ZERO, node.size, color)
    }
}