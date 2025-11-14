package top.fifthlight.combine.layout.measure

import androidx.compose.runtime.Stable
import top.fifthlight.combine.layout.constraints.Constraints
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize
import top.fifthlight.data.Offset

data class MeasureResult(
    val width: Int,
    val height: Int,
    val placer: Placer,
)

@Stable
fun interface MeasurePolicy {
    fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult

    fun measure(measurables: List<Measurable>, constraints: Constraints) =
        with(MeasureScope) { this.measure(measurables, constraints) }

    fun MeasureScope.minIntrinsicWidth(measurables: List<Measurable>, height: Int): Int =
        throw UnsupportedOperationException("minIntrinsicWidth not implemented")

    fun MeasureScope.minIntrinsicHeight(measurables: List<Measurable>, width: Int): Int =
        throw UnsupportedOperationException("minIntrinsicHeight not implemented")

    fun MeasureScope.maxIntrinsicWidth(measurables: List<Measurable>, height: Int): Int =
        throw UnsupportedOperationException("maxIntrinsicWidth not implemented")

    fun MeasureScope.maxIntrinsicHeight(measurables: List<Measurable>, width: Int): Int =
        throw UnsupportedOperationException("maxIntrinsicHeight not implemented")

    fun minIntrinsicWidth(measurables: List<Measurable>, height: Int): Int =
        with(MeasureScope) { this.minIntrinsicWidth(measurables, height) }

    fun maxIntrinsicWidth(measurables: List<Measurable>, height: Int): Int =
        with(MeasureScope) { this.maxIntrinsicWidth(measurables, height) }

    fun minIntrinsicHeight(measurables: List<Measurable>, width: Int): Int =
        with(MeasureScope) { this.minIntrinsicHeight(measurables, width) }

    fun maxIntrinsicHeight(measurables: List<Measurable>, width: Int): Int =
        with(MeasureScope) { this.maxIntrinsicHeight(measurables, width) }
}

object MeasureScope {
    fun layout(
        width: Int,
        height: Int,
        placer: Placer
    ) = MeasureResult(
        width = width, height = height, placer = placer
    )

    fun layout(size: IntSize, placer: Placer) = layout(size.width, size.height, placer)
}

@Stable
fun interface Placer {
    fun placeChildren()
}

interface Measurable {
    val parentData: Any?
    fun measure(constraints: Constraints): Placeable

    fun minIntrinsicWidth(height: Int): Int
    fun minIntrinsicHeight(width: Int): Int
    fun maxIntrinsicWidth(height: Int): Int
    fun maxIntrinsicHeight(width: Int): Int
}

interface Placeable {
    val x: Int
    val y: Int
    val absoluteX: Int
    val absoluteY: Int
    val width: Int
    val height: Int

    fun placeAt(x: Int, y: Int)
    fun placeAt(offset: IntOffset) = placeAt(offset.x, offset.y)

    val position get() = IntOffset(x, y)
    val absolutePosition get() = IntOffset(absoluteX, absoluteY)
    val size get() = IntSize(width, height)
}

operator fun Placeable.contains(offset: Offset): Boolean {
    val xInRange = absoluteX <= offset.x && offset.x < absoluteX + width
    val yInRange = absoluteY <= offset.y && offset.y < absoluteY + height
    return xInRange && yInRange
}
