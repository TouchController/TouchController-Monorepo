package top.fifthlight.combine.modifier.drawing

import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.translate
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize

fun Modifier.clip() = then(ClipNode)

private data object ClipNode : DrawModifierNode, Modifier.Node<ClipNode> {
    override fun Canvas.renderBefore(node: Placeable) {
        pushClip(
            IntRect(
                offset = IntOffset(node.absoluteX, node.absoluteY),
                size = node.size,
            ),
            IntRect(
                offset = IntOffset(node.x, node.y),
                size = node.size,
            ),
        )
    }

    override fun Canvas.renderAfter(node: Placeable) {
        popClip()
    }
}


fun Modifier.clip(
    width: Float,
    height: Float,
    anchorOffset: IntOffset? = null
) = then(PercentClipNode(width, height, anchorOffset))

private data class PercentClipNode(
    val width: Float,
    val height: Float,
    val anchorOffset: IntOffset? = null
) : DrawModifierNode, Modifier.Node<PercentClipNode> {
    override fun Canvas.renderBefore(
        node: Placeable
    ) {
        val size = IntSize(
            width = (node.width * width).toInt(),
            height = (node.height * height).toInt(),
        )
        val offset = anchorOffset?.let {
            IntOffset(
                x = if (node.absoluteX > anchorOffset.x) {
                    node.width - size.width
                } else {
                    0
                },
                y = if (node.absoluteY > anchorOffset.y) {
                    size.height - node.height
                } else {
                    0
                }
            )
        } ?: IntOffset.ZERO
        pushClip(
            IntRect(
                offset = IntOffset(node.absoluteX, node.absoluteY),
                size = size,
            ),
            IntRect(
                offset = IntOffset.ZERO,
                size = size,
            ),
        )
        translate(offset)
    }

    override fun Canvas.renderAfter(node: Placeable) {
        popClip()
    }
}