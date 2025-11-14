package top.fifthlight.combine.modifier.drawing

import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.paint.Canvas

fun Modifier.rotate(degrees: Float) = then(RotateModifierNode(degrees))

private data class RotateModifierNode(
    val degrees: Float
) : DrawModifierNode, Modifier.Node<RotateModifierNode> {
    override fun Canvas.renderBefore(node: Placeable) {
        pushState()
        translate(node.width / 2, node.height / 2)
        rotate(degrees)
        translate(-node.width / 2, -node.height / 2)
    }

    override fun Canvas.renderAfter(node: Placeable) {
        popState()
    }
}
