package top.fifthlight.touchcontroller.layout

import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.Color
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.Rect

fun Context.Texture(texture: Texture, srcRect: IntRect = IntRect(IntOffset.ZERO, texture.size)) {
    if (opacity == 1f) {
        drawQueue.enqueue { canvas ->
            canvas.drawTexture(
                texture = texture,
                dstRect = Rect(size = size.toSize()),
                srcRect = srcRect,
            )
        }
    } else {
        val color = Color(((0xFF * opacity).toInt() shl 24) or 0xFFFFFF)
        drawQueue.enqueue { canvas ->
            canvas.drawTexture(
                texture = texture,
                dstRect = Rect(size = size.toSize()),
                srcRect = srcRect,
                tint = color
            )
        }
    }
}

fun Context.Texture(texture: Texture, srcRect: IntRect = IntRect(IntOffset.ZERO, texture.size), color: UInt) {
    if (opacity == 1f) {
        drawQueue.enqueue { canvas ->
            canvas.drawTexture(
                texture = texture,
                dstRect = Rect(size = size.toSize()),
                srcRect = srcRect,
                tint = Color(color)
            )
        }
    } else {
        val colorWithoutAlpha = color.toInt() and 0xFFFFFF
        val colorWithAlpha = Color(((0xFF * opacity).toInt() shl 24) or colorWithoutAlpha)
        drawQueue.enqueue { canvas ->
            canvas.drawTexture(
                texture = texture,
                dstRect = Rect(size = size.toSize()),
                srcRect = srcRect,
                tint = colorWithAlpha,
            )
        }
    }
}