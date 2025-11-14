package top.fifthlight.touchcontroller.common.helper

import top.fifthlight.data.IntSize
import top.fifthlight.data.Offset

fun Offset.fixAspectRadio(windowSize: IntSize): Offset = Offset(
    x = x,
    y = y * windowSize.height / windowSize.width
)