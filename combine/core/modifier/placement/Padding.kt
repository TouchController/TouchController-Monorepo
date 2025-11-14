package top.fifthlight.combine.modifier.placement

import top.fifthlight.combine.layout.measure.Measurable
import top.fifthlight.combine.layout.measure.MeasureResult
import top.fifthlight.combine.layout.measure.MeasureScope
import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.combine.layout.constraints.offset
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.LayoutModifierNode
import top.fifthlight.data.IntPadding

fun Modifier.padding(size: IntPadding): Modifier = padding(size.left, size.top, size.right, size.bottom)

fun Modifier.padding(size: Int = 0): Modifier = padding(size, size)

fun Modifier.padding(width: Int = 0, height: Int = 0): Modifier = padding(width, height, width, height)

fun Modifier.padding(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): Modifier =
    then(PaddingNode(left, top, right, bottom))

private data class PaddingNode(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) : LayoutModifierNode, Modifier.Node<PaddingNode> {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val horizontalPadding = left + right
        val verticalPadding = top + bottom
        val adjustedConstraints = constraints.offset(-horizontalPadding, -verticalPadding)

        val placeable = measurable.measure(adjustedConstraints)
        val width = (placeable.width + horizontalPadding).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height + verticalPadding).coerceIn(constraints.minHeight, constraints.maxHeight)

        return layout(width, height) {
            placeable.placeAt(left, top)
        }
    }

    override fun MeasureScope.minIntrinsicWidth(measurable: Measurable, height: Int): Int {
        val horizontalPadding = left + right
        val verticalPadding = top + bottom
        return measurable.minIntrinsicWidth(height - verticalPadding) + horizontalPadding
    }

    override fun MeasureScope.maxIntrinsicWidth(measurable: Measurable, height: Int): Int {
        val horizontalPadding = left + right
        val verticalPadding = top + bottom
        return measurable.maxIntrinsicWidth(height - verticalPadding) + horizontalPadding
    }

    override fun MeasureScope.minIntrinsicHeight(measurable: Measurable, width: Int): Int {
        val horizontalPadding = left + right
        val verticalPadding = top + bottom
        return measurable.minIntrinsicHeight(width - horizontalPadding) + verticalPadding
    }

    override fun MeasureScope.maxIntrinsicHeight(measurable: Measurable, width: Int): Int {
        val horizontalPadding = left + right
        val verticalPadding = top + bottom
        return measurable.maxIntrinsicHeight(width - horizontalPadding) + verticalPadding
    }
}
