package top.fifthlight.combine.modifier.placement

import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.combine.layout.measure.Measurable
import top.fifthlight.combine.layout.measure.MeasureResult
import top.fifthlight.combine.layout.measure.MeasureScope
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.LayoutModifierNode

fun Modifier.fillMaxSize(fraction: Float = 1f) = then(FillNode(width = fraction, height = fraction))

fun Modifier.fillMaxWidth(fraction: Float = 1f) = then(FillNode(width = fraction))

fun Modifier.fillMaxHeight(fraction: Float = 1f) = then(FillNode(height = fraction))

private data class FillNode(
    val width: Float? = null,
    val height: Float? = null,
) : LayoutModifierNode, Modifier.Node<FillNode> {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        fun transform(fraction: Float, size: Int): Int = if (size == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            (size * fraction).toInt()
        }

        var targetConstraints = constraints
        if (width != null) {
            val width = transform(width, targetConstraints.maxWidth).coerceIn(
                targetConstraints.minWidth,
                targetConstraints.maxWidth
            )
            targetConstraints = targetConstraints.copy(
                minWidth = width,
                maxWidth = width,
            )
        }
        if (height != null) {
            val height = transform(height, targetConstraints.maxHeight).coerceIn(
                targetConstraints.minHeight,
                targetConstraints.maxHeight
            )
            targetConstraints = targetConstraints.copy(
                minHeight = height,
                maxHeight = height
            )
        }

        val placeable = measurable.measure(targetConstraints)

        return layout(placeable.width, placeable.height) {
            placeable.placeAt(0, 0)
        }
    }

    override fun MeasureScope.minIntrinsicWidth(measurable: Measurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun MeasureScope.maxIntrinsicWidth(measurable: Measurable, height: Int): Int {
        return if (width != null) {
            Int.MAX_VALUE
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun MeasureScope.minIntrinsicHeight(measurable: Measurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun MeasureScope.maxIntrinsicHeight(measurable: Measurable, width: Int): Int {
        return if (height != null) {
            Int.MAX_VALUE
        } else {
            measurable.maxIntrinsicHeight(width)
        }
    }
}
