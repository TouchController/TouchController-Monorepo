package top.fifthlight.touchcontroller.common.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize

@Serializable
enum class Align(val alignment: Alignment) {
    @SerialName("left_top")
    LEFT_TOP(Alignment.TopLeft),

    @SerialName("center_top")
    CENTER_TOP(Alignment.TopCenter),

    @SerialName("right_top")
    RIGHT_TOP(Alignment.TopRight),

    @SerialName("left_center")
    LEFT_CENTER(Alignment.CenterLeft),

    @SerialName("center_center")
    CENTER_CENTER(Alignment.Center),

    @SerialName("right_center")
    RIGHT_CENTER(Alignment.CenterRight),

    @SerialName("left_bottom")
    LEFT_BOTTOM(Alignment.BottomLeft),

    @SerialName("center_bottom")
    CENTER_BOTTOM(Alignment.BottomCenter),

    @SerialName("right_bottom")
    RIGHT_BOTTOM(Alignment.BottomRight);

    fun normalizeOffset(offset: IntOffset) = when (this) {
        LEFT_TOP, CENTER_TOP, LEFT_CENTER, CENTER_CENTER -> offset
        RIGHT_TOP, RIGHT_CENTER -> IntOffset(-offset.x, offset.y)
        LEFT_BOTTOM, CENTER_BOTTOM -> IntOffset(offset.x, -offset.y)
        RIGHT_BOTTOM -> -offset
    }

    fun alignOffset(windowSize: IntSize, size: IntSize, offset: IntOffset) = when (this) {
        LEFT_TOP -> offset

        CENTER_TOP -> IntOffset(
            x = (windowSize.width - size.width) / 2 + offset.x,
            y = offset.y
        )

        RIGHT_TOP -> IntOffset(
            x = windowSize.width - size.width - offset.x,
            y = offset.y,
        )

        LEFT_CENTER -> IntOffset(
            x = offset.x,
            y = (windowSize.height - size.height) / 2 + offset.y
        )

        CENTER_CENTER -> (windowSize - size) / 2 + offset

        RIGHT_CENTER -> IntOffset(
            x = windowSize.width - size.width - offset.x,
            y = (windowSize.height - size.height) / 2 + offset.y
        )

        LEFT_BOTTOM -> IntOffset(
            x = offset.x,
            y = windowSize.height - size.height - offset.y,
        )

        CENTER_BOTTOM -> IntOffset(
            x = (windowSize.width - size.width) / 2 + offset.x,
            y = windowSize.height - size.height - offset.y,
        )

        RIGHT_BOTTOM -> IntOffset(
            x = windowSize.width - size.width - offset.x,
            y = windowSize.height - size.height - offset.y,
        )
    }
}

inline fun <reified T> Context.withAlign(
    align: Align,
    size: IntSize,
    offset: IntOffset = IntOffset.ZERO,
    crossinline block: Context.() -> T
): T =
    withRect(
        offset = align.alignOffset(windowSize = this.size, offset = offset, size = size),
        size = size,
        block = block
    )