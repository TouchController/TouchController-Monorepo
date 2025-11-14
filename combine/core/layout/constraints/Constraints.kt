package top.fifthlight.combine.layout.constraints

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import top.fifthlight.data.IntSize

@Immutable
class Constraints(
    val minWidth: Int = 0,
    val maxWidth: Int = Int.MAX_VALUE,
    val minHeight: Int = 0,
    val maxHeight: Int = Int.MAX_VALUE
) {
    fun copy(
        minWidth: Int = this.minWidth,
        maxWidth: Int = this.maxWidth,
        minHeight: Int = this.minHeight,
        maxHeight: Int = this.maxHeight
    ) = Constraints(
        minWidth.coerceAtMost(maxWidth),
        maxWidth.coerceAtLeast(minWidth),
        minHeight.coerceAtMost(maxHeight),
        maxHeight.coerceAtLeast(minHeight)
    )
}

@Stable
fun Constraints.offset(horizontal: Int = 0, vertical: Int = 0) = Constraints(
    (minWidth + horizontal).coerceAtLeast(0),
    addMaxWithMinimum(maxWidth, horizontal),
    (minHeight + vertical).coerceAtLeast(0),
    addMaxWithMinimum(maxHeight, vertical),
)

@Stable
fun IntSize.coerceIn(constraints: Constraints) = IntSize(
    width = width.coerceIn(constraints.minWidth, constraints.maxWidth),
    height = height.coerceIn(constraints.minHeight, constraints.maxHeight),
)

private fun addMaxWithMinimum(max: Int, value: Int): Int {
    return if (max == Int.MAX_VALUE) {
        max
    } else {
        (max + value).coerceAtLeast(0)
    }
}
