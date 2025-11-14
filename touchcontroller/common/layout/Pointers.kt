package top.fifthlight.touchcontroller.common.layout

import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.paint.drawCenteredText
import top.fifthlight.combine.paint.withTranslate
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize

fun Context.Pointers() {
    drawQueue.enqueue { canvas ->
        pointers.forEach { (id, pointer) ->
            canvas.withTranslate(pointer.scaledOffset) {
                fillRect(IntOffset(-1, -1), IntSize(2, 2), Colors.WHITE)
                drawRect(IntOffset(-4, -4), IntSize(8, 8), Colors.WHITE)
                drawCenteredText(
                    offset = IntOffset(0, 9),
                    textMeasurer = textMeasurer,
                    text = id.toString(),
                    color = Colors.WHITE,
                )
            }
        }
    }
}