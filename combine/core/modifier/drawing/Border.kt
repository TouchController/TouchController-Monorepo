package top.fifthlight.combine.modifier.drawing

import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.combine.layout.constraints.coerceIn
import top.fifthlight.combine.layout.constraints.offset
import top.fifthlight.combine.layout.measure.Measurable
import top.fifthlight.combine.layout.measure.MeasureResult
import top.fifthlight.combine.layout.measure.MeasureScope
import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.node.WrapperFactory
import top.fifthlight.combine.node.plus
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Drawable
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize

fun Modifier.border(size: Int = 0, color: Color): Modifier = border(size, size, color)

fun Modifier.border(width: Int = 0, height: Int = 0, color: Color): Modifier = border(width, height, width, height, color)

fun Modifier.border(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0, color: Color): Modifier =
    then(BorderNode(left, top, right, bottom, color))

private data class BorderNode(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val color: Color,
) : DrawModifierNode, LayoutModifierNode, Modifier.Node<BorderNode> {
    override fun Canvas.renderAfter(node: Placeable) {
        if (left > 0) {
            fillRect(
                offset = IntOffset(0, 0),
                size = IntSize(left, node.height),
                color = color
            )
        }
        if (top > 0) {
            fillRect(
                offset = IntOffset(0, 0),
                size = IntSize(node.width, top),
                color = color
            )
        }
        if (right > 0) {
            fillRect(
                offset = IntOffset(node.width - right, 0),
                size = IntSize(right, node.height),
                color = color
            )
        }
        if (bottom > 0) {
            fillRect(
                offset = IntOffset(0, node.height - bottom),
                size = IntSize(node.width, bottom),
                color = color
            )
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val horizontalBorder = left + right
        val verticalBorder = top + bottom
        val adjustedConstraints = constraints.offset(-horizontalBorder, -verticalBorder)

        val placeable = measurable.measure(adjustedConstraints)
        val width = (placeable.width + horizontalBorder).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height + verticalBorder).coerceIn(constraints.minHeight, constraints.maxHeight)

        return layout(width, height) {
            placeable.placeAt(left, top)
        }
    }

    override fun MeasureScope.minIntrinsicWidth(measurable: Measurable, height: Int): Int {
        val horizontalBorder = left + right
        val verticalBorder = top + bottom
        return measurable.minIntrinsicWidth(height - verticalBorder) + horizontalBorder
    }

    override fun MeasureScope.maxIntrinsicWidth(measurable: Measurable, height: Int): Int {
        val horizontalBorder = left + right
        val verticalBorder = top + bottom
        return measurable.maxIntrinsicWidth(height - verticalBorder) + horizontalBorder
    }

    override fun MeasureScope.minIntrinsicHeight(measurable: Measurable, width: Int): Int {
        val horizontalBorder = left + right
        val verticalBorder = top + bottom
        return measurable.minIntrinsicHeight(width - horizontalBorder) + verticalBorder
    }

    override fun MeasureScope.maxIntrinsicHeight(measurable: Measurable, width: Int): Int {
        val horizontalBorder = left + right
        val verticalBorder = top + bottom
        return measurable.maxIntrinsicHeight(width - horizontalBorder) + verticalBorder
    }

    companion object {
        val wrapperFactory = LayoutModifierNode.wrapperFactory + DrawModifierNode.wrapperFactory
    }

    override val wrapperFactory: WrapperFactory<*>
        get() = Companion.wrapperFactory
}

fun Modifier.border(drawable: Drawable): Modifier = then(DrawableBorderNode(drawable))

private data class DrawableBorderNode(
    val drawable: Drawable,
) : DrawModifierNode, LayoutModifierNode, Modifier.Node<DrawableBorderNode> {
    override fun Canvas.renderBefore(node: Placeable) {
        drawable.run { draw(IntRect(offset = IntOffset.ZERO, size = node.size)) }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val padding = drawable.padding
        val adjustedConstraints = constraints.offset(-padding.width, -padding.height)

        val placeable = measurable.measure(adjustedConstraints)
        val size = (placeable.size + padding.size).coerceIn(constraints)

        return layout(size) {
            placeable.placeAt(padding.left, padding.top)
        }
    }

    override fun MeasureScope.minIntrinsicWidth(measurable: Measurable, height: Int): Int {
        val horizontalBorder = drawable.padding.width
        val verticalBorder = drawable.padding.height
        return measurable.minIntrinsicWidth(height - verticalBorder) + horizontalBorder
    }

    override fun MeasureScope.maxIntrinsicWidth(measurable: Measurable, height: Int): Int {
        val horizontalBorder = drawable.padding.width
        val verticalBorder = drawable.padding.height
        return measurable.maxIntrinsicWidth(height - verticalBorder) + horizontalBorder
    }

    override fun MeasureScope.minIntrinsicHeight(measurable: Measurable, width: Int): Int {
        val horizontalBorder = drawable.padding.width
        val verticalBorder = drawable.padding.height
        return measurable.minIntrinsicHeight(width - horizontalBorder) + verticalBorder
    }

    override fun MeasureScope.maxIntrinsicHeight(measurable: Measurable, width: Int): Int {
        val horizontalBorder = drawable.padding.width
        val verticalBorder = drawable.padding.height
        return measurable.maxIntrinsicHeight(width - horizontalBorder) + verticalBorder
    }

    companion object {
        val wrapperFactory = LayoutModifierNode.wrapperFactory + DrawModifierNode.wrapperFactory
    }

    override val wrapperFactory: WrapperFactory<*>
        get() = Companion.wrapperFactory
}


