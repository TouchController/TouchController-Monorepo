package top.fifthlight.touchcontroller.resource

import kotlinx.serialization.Serializable
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import java.awt.image.BufferedImage

private data class LineData(
    val start: Int,
    val length: Int,
)

private sealed class FindLineState {
    data object StartPadding : FindLineState()

    data class InLine(
        val start: Int,
    ) : FindLineState()

    data class EndPadding(
        val start: Int,
        val length: Int
    ) : FindLineState()
}

private fun findLine(
    bitmap: BufferedImage,
    values: Iterable<Int>,
    coordinateConvertor: (Int) -> IntOffset
): LineData? {
    var state: FindLineState = FindLineState.StartPadding

    fun checkBlackPixel(color: Int, x: Int, y: Int) {
        check(color == 0) {
            "Color at ($x, $y) is not black: $color"
        }
    }

    for (value in values) {
        val (x, y) = coordinateConvertor(value)
        val argb = bitmap.getRGB(x, y)
        val alpha = argb shr 24
        val color = argb and 0xFFFFFF
        when (state) {
            FindLineState.StartPadding -> {
                if (alpha == 0) {
                    continue
                }
                checkBlackPixel(color, x, y)
                state = FindLineState.InLine(value)
            }

            is FindLineState.InLine -> {
                if (alpha != 0) {
                    checkBlackPixel(color, x, y)
                    continue
                }
                state = FindLineState.EndPadding(
                    start = state.start,
                    length = value - state.start,
                )
            }

            is FindLineState.EndPadding -> {
                if (alpha == 0) {
                    continue
                }
                error("Bad pixel at ($x, $y): A $alpha RGB $color (Alpha should be 0)")
            }
        }
    }
    return if (state is FindLineState.EndPadding) {
        val (start, length) = state
        LineData(start, length)
    } else {
        null
    }
}

fun NinePatch(bitmap: BufferedImage): NinePatch {
    val topLine = findLine(bitmap, 0 until bitmap.width) { x -> IntOffset(x, 0) }
        ?: throw IllegalArgumentException("NinePatch image is missing the top stretch line.")
    val leftLine = findLine(bitmap, 0 until bitmap.height) { y -> IntOffset(0, y) }
        ?: throw IllegalArgumentException("NinePatch image is missing the left stretch line.")

    val scaleArea = IntRect(
        offset = IntOffset(
            x = topLine.start - 1,
            y = leftLine.start - 1,
        ),
        size = IntSize(
            width = topLine.length,
            height = leftLine.length,
        )
    )

    val bottomLine = findLine(bitmap, 0 until bitmap.width) { x -> IntOffset(x, bitmap.height - 1) }
    val rightLine = findLine(bitmap, 0 until bitmap.height) { y -> IntOffset(bitmap.width - 1, y) }

    val paddingLeft = bottomLine?.start?.let { it - 1 } ?: 0
    val paddingRight = bottomLine?.let { bottomLine -> bitmap.width - bottomLine.start - bottomLine.length - 1 } ?: 0
    val paddingTop = rightLine?.start?.let { it - 1 } ?: 0
    val paddingBottom = rightLine?.let { rightLine -> bitmap.height - rightLine.start - rightLine.length - 1 } ?: 0

    val padding = IntPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    return NinePatch(scaleArea, padding)
}

@Serializable
data class NinePatch(
    val scaleArea: IntRect,
    val padding: IntPadding
)
