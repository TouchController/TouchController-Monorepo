package top.fifthlight.combine.item

import top.fifthlight.combine.paint.Canvas
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntSize

interface ItemCanvas: Canvas {
    fun drawItemStack(offset: IntOffset, size: IntSize = IntSize(16), stack: ItemStack)
}