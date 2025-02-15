package top.fifthlight.combine.modifier.drawing

import top.fifthlight.combine.data.NinePatchTexture
import top.fifthlight.combine.layout.Measurable
import top.fifthlight.combine.layout.MeasureResult
import top.fifthlight.combine.layout.MeasureScope
import top.fifthlight.combine.layout.Placeable
import top.fifthlight.combine.modifier.*
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.paint.RenderContext
import top.fifthlight.combine.paint.drawNinePatchTexture
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect

fun Modifier.border(texture: NinePatchTexture, color: Color = Colors.WHITE): Modifier =
    then(NinePatchBorderNode(texture, color))

private data class NinePatchBorderNode(
    val texture: NinePatchTexture,
    val color: Color,
) : DrawModifierNode, LayoutModifierNode, Modifier.Node<NinePatchBorderNode> {
    override fun renderBeforeContext(context: RenderContext, node: Placeable) {
        context.canvas.drawNinePatchTexture(texture, IntRect(IntOffset.ZERO, node.size))
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val adjustedConstraints = constraints.offset(-texture.padding.width, -texture.padding.height)

        val placeable = measurable.measure(adjustedConstraints)
        val size = (placeable.size + texture.padding.size).coerceIn(constraints)

        return layout(size) {
            placeable.placeAt(texture.padding.left, texture.padding.top)
        }
    }
}

