package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import top.fifthlight.combine.layout.Measurable
import top.fifthlight.combine.layout.MeasurePolicy
import top.fifthlight.combine.layout.MeasureResult
import top.fifthlight.combine.layout.MeasureScope
import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.paint.Drawable
import top.fifthlight.combine.widget.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize

sealed class ContentScale {
    data object Fit : ContentScale()
    data object Fill : ContentScale()
    data object Center : ContentScale()
}

@Composable
fun Icon(
    drawable: Drawable,
    modifier: Modifier = Modifier,
    size: IntSize = drawable.size,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val contentAspect = size.width.toFloat() / size.height.toFloat()
    Canvas(
        modifier = modifier,
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
                return layout(
                    width = size.width.coerceIn(constraints.minWidth, constraints.maxWidth),
                    height = size.height.coerceIn(constraints.minHeight, constraints.maxHeight),
                ) {
                }
            }

            override fun MeasureScope.minIntrinsicWidth(measurables: List<Measurable>, height: Int): Int = size.width

            override fun MeasureScope.maxIntrinsicWidth(measurables: List<Measurable>, height: Int): Int = size.width

            override fun MeasureScope.minIntrinsicHeight(measurables: List<Measurable>, width: Int): Int = size.height

            override fun MeasureScope.maxIntrinsicHeight(measurables: List<Measurable>, width: Int): Int = size.height
        },
    ) { node ->
        val nodeWidth = node.width
        val nodeHeight = node.height
        val nodeAspect = nodeWidth.toFloat() / nodeHeight.toFloat()

        val renderRect = when (contentScale) {
            ContentScale.Fit -> {
                val scaledWidth: Int
                val scaledHeight: Int

                if (contentAspect > nodeAspect) {
                    scaledWidth = nodeWidth
                    scaledHeight = (nodeWidth / contentAspect).toInt()
                } else {
                    scaledHeight = nodeHeight
                    scaledWidth = (nodeHeight * contentAspect).toInt()
                }

                IntRect(
                    offset = (node.size - IntSize(scaledWidth, scaledHeight)) / 2,
                    size = IntSize(scaledWidth, scaledHeight),
                )
            }

            ContentScale.Fill -> {
                IntRect(
                    offset = IntOffset.ZERO,
                    size = node.size,
                )
            }

            ContentScale.Center -> {
                val targetWidth: Int
                val targetHeight: Int

                if (size.width <= nodeWidth && size.height <= nodeHeight) {
                    targetWidth = size.width
                    targetHeight = size.height
                } else {
                    if (contentAspect > nodeAspect) {
                        targetWidth = nodeWidth
                        targetHeight = (nodeWidth / contentAspect).toInt()
                    } else {
                        targetHeight = nodeHeight
                        targetWidth = (nodeHeight * contentAspect).toInt()
                    }
                }

                IntRect(
                    offset = (node.size - IntSize(targetWidth, targetHeight)) / 2,
                    size = IntSize(targetWidth, targetHeight),
                )
            }
        }

        drawable.run { draw(renderRect) }
    }
}