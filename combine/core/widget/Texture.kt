package top.fifthlight.combine.widget

import androidx.compose.runtime.Composable
import top.fifthlight.combine.paint.Texture
import top.fifthlight.combine.layout.*
import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.layout.measure.*
import top.fifthlight.data.*

@Composable
fun Texture(
    texture: Texture,
    srcRect: IntRect = IntRect(IntOffset.ZERO, texture.size),
    size: IntSize,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult =
                layout(
                    width = size.width.coerceIn(constraints.minWidth, constraints.maxWidth),
                    height = size.height.coerceIn(constraints.minHeight, constraints.maxHeight)
                ) {}

            override fun MeasureScope.minIntrinsicWidth(measurables: List<Measurable>, height: Int): Int = size.width
            override fun MeasureScope.minIntrinsicHeight(measurables: List<Measurable>, width: Int): Int = size.height
            override fun MeasureScope.maxIntrinsicWidth(measurables: List<Measurable>, height: Int): Int = size.width
            override fun MeasureScope.maxIntrinsicHeight(measurables: List<Measurable>, width: Int): Int = size.height
        },
        renderer = {
            with(texture) {
                draw(
                    dstRect = IntRect(offset = IntOffset.ZERO, size = size),
                    srcRect = srcRect,
                )
            }
        }
    )
}