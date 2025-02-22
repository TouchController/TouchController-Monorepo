package top.fifthlight.combine.paint

import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.NinePatchTexture
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.data.Texture
import top.fifthlight.data.*

enum class BlendFactor {
    ONE,
    ZERO,
    SRC_COLOR,
    SRC_ALPHA,
    ONE_MINUS_SRC_ALPHA,
    ONE_MINUS_SRC_COLOR,
    DST_COLOR,
    DST_ALPHA,
    ONE_MINUS_DST_ALPHA,
    ONE_MINUS_DST_COLOR,
}

data class BlendFunction(
    val srcFactor: BlendFactor,
    val dstFactor: BlendFactor,
    val srcAlpha: BlendFactor,
    val dstAlpha: BlendFactor
)

inline fun Canvas.withBlend(crossinline block: Canvas.() -> Unit) {
    val enabled = blendEnabled
    if (!enabled) {
        enableBlend()
    }
    try {
        block()
    } finally {
        if (!enabled) {
            disableBlend()
        }
    }
}

inline fun Canvas.withBlendFunction(
    func: BlendFunction,
    crossinline block: () -> Unit
) {
    blendFunction(func)
    try {
        block()
    } finally {
        defaultBlendFunction()
    }
}

enum class GuiTexture {
    BUTTON,
    BUTTON_HOVER,
    BUTTON_ACTIVE,
    BUTTON_DISABLED,
}

interface Canvas {
    val textLineHeight: Int
    var blendEnabled: Boolean

    fun pushState()
    fun popState()
    fun translate(x: Int, y: Int)
    fun translate(x: Float, y: Float)
    fun rotate(degrees: Float)
    fun scale(x: Float, y: Float)
    fun fillRect(offset: IntOffset = IntOffset.ZERO, size: IntSize = IntSize.ZERO, color: Color)
    fun drawRect(offset: IntOffset = IntOffset.ZERO, size: IntSize = IntSize.ZERO, color: Color)
    fun drawText(offset: IntOffset, text: String, color: Color)
    fun drawText(offset: IntOffset, width: Int, text: String, color: Color)
    fun drawText(offset: IntOffset, text: Text, color: Color)
    fun drawText(offset: IntOffset, width: Int, text: Text, color: Color)
    fun drawTexture(
        texture: Texture,
        dstRect: Rect,
        srcRect: IntRect = IntRect(IntOffset.ZERO, texture.size),
        tint: Color = Colors.WHITE,
    )

    fun drawGuiTexture(texture: GuiTexture, dstRect: IntRect)
    fun drawItemStack(offset: IntOffset, size: IntSize = IntSize(16), stack: ItemStack)
    fun enableBlend()
    fun disableBlend()
    fun blendFunction(func: BlendFunction)
    fun defaultBlendFunction()
    fun pushClip(absoluteArea: IntRect, relativeArea: IntRect)
    fun popClip()
}

fun Canvas.translate(offset: IntOffset) = translate(offset.x, offset.y)
fun Canvas.translate(offset: Offset) = translate(offset.x, offset.y)

fun Canvas.drawNinePatchTexture(
    texture: NinePatchTexture,
    dstRect: IntRect,
    tint: Color = Colors.WHITE,
) {
    val textureSize = texture.size
    val scaleArea = texture.scaleArea
    val dstScaleAreaSize = dstRect.size - (texture.size - scaleArea.size)
    val srcBottomRightCornerOffset = scaleArea.offset + IntOffset(scaleArea.size.width, scaleArea.size.height)
    val dstBottomRightCornerOffset = dstRect.size - (textureSize - srcBottomRightCornerOffset)

    fun drawRegion(src: IntRect, dst: IntRect) {
        drawTexture(
            texture = texture,
            dstRect = dst.toRect(),
            srcRect = src,
            tint = tint
        )
    }

    // Top-left corner
    drawRegion(
        src = IntRect(
            offset = IntOffset.ZERO,
            size = IntSize(scaleArea.offset.left, scaleArea.offset.top)
        ),
        dst = IntRect(
            offset = dstRect.offset,
            size = IntSize(scaleArea.offset.left, scaleArea.offset.top)
        )
    )

    // Top edge
    drawRegion(
        src = IntRect(
            offset = IntOffset(scaleArea.left, 0),
            size = IntSize(scaleArea.size.width, scaleArea.top)
        ),
        dst = IntRect(
            offset = dstRect.offset + IntOffset(scaleArea.left, 0),
            size = IntSize(dstScaleAreaSize.width, scaleArea.top)
        )
    )

    // Top-right corner
    drawRegion(
        src = IntRect(
            offset = IntOffset(scaleArea.right, 0),
            size = IntSize(textureSize.width - scaleArea.right, scaleArea.top)
        ),
        dst = IntRect(
            offset = dstRect.offset + IntOffset(dstBottomRightCornerOffset.left, dstRect.top),
            size = IntSize(textureSize.width - scaleArea.right, scaleArea.top)
        )
    )

    // Middle-left edge
    drawRegion(
        src = IntRect(
            offset = IntOffset(0, scaleArea.top),
            size = IntSize(scaleArea.left, scaleArea.size.height)
        ),
        dst = IntRect(
            offset = dstRect.offset + IntOffset(0, scaleArea.top),
            size = IntSize(scaleArea.left, dstScaleAreaSize.height)
        )
    )

    // Middle-center (scale area)
    drawRegion(
        src = scaleArea,
        dst = IntRect(
            offset = dstRect.offset + scaleArea.offset,
            size = dstScaleAreaSize
        )
    )

    // Middle-right edge
    drawRegion(
        src = IntRect(
            offset = IntOffset(scaleArea.right, scaleArea.top),
            size = IntSize(textureSize.width - scaleArea.right, scaleArea.size.height)
        ),
        dst = IntRect(
            offset = dstRect.offset + IntOffset(dstBottomRightCornerOffset.x, scaleArea.top),
            size = IntSize(textureSize.width - scaleArea.right, dstScaleAreaSize.height)
        )
    )

    // Bottom-left corner
    drawRegion(
        src = IntRect(
            offset = IntOffset(0, scaleArea.bottom),
            size = IntSize(scaleArea.left, textureSize.height - scaleArea.bottom)
        ),
        dst = IntRect(
            offset = dstRect.offset + IntOffset(0, scaleArea.top + dstScaleAreaSize.height),
            size = IntSize(scaleArea.left, textureSize.height - scaleArea.bottom)
        )
    )

    // Bottom edge
    drawRegion(
        src = IntRect(
            offset = IntOffset(scaleArea.left, scaleArea.bottom),
            size = IntSize(scaleArea.size.width, textureSize.height - scaleArea.bottom)
        ),
        dst = IntRect(
            offset = dstRect.offset + IntOffset(scaleArea.left, scaleArea.top + dstScaleAreaSize.height),
            size = IntSize(dstScaleAreaSize.width, textureSize.height - scaleArea.bottom)
        )
    )

    // Bottom-right corner
    drawRegion(
        src = IntRect(
            offset = IntOffset(scaleArea.right, scaleArea.bottom),
            size = IntSize(textureSize.width - scaleArea.right, textureSize.height - scaleArea.bottom)
        ),
        dst = IntRect(
            offset = dstRect.offset + IntOffset(dstBottomRightCornerOffset.x, scaleArea.top + dstScaleAreaSize.height),
            size = IntSize(textureSize.width - scaleArea.right, textureSize.height - scaleArea.bottom)
        )
    )
}

inline fun Canvas.withTranslate(x: Int, y: Int, crossinline block: Canvas.() -> Unit) {
    translate(x, y)
    try {
        block()
    } finally {
        translate(-x, -y)
    }
}

inline fun Canvas.withTranslate(x: Float, y: Float, crossinline block: Canvas.() -> Unit) {
    translate(x, y)
    try {
        block()
    } finally {
        translate(-x, -y)
    }
}

inline fun Canvas.withTranslate(offset: IntOffset, crossinline block: Canvas.() -> Unit) {
    translate(offset)
    try {
        block()
    } finally {
        translate(-offset)
    }
}

inline fun Canvas.withTranslate(offset: Offset, crossinline block: Canvas.() -> Unit) {
    translate(offset)
    try {
        block()
    } finally {
        translate(-offset)
    }
}

fun Canvas.scale(scale: Float) = scale(scale, scale)

inline fun Canvas.withScale(scale: Float, crossinline block: Canvas.() -> Unit) {
    pushState()
    scale(scale)
    try {
        block()
    } finally {
        popState()
    }
}

inline fun Canvas.withScale(x: Float, y: Float, crossinline block: Canvas.() -> Unit) {
    pushState()
    scale(x, y)
    try {
        block()
    } finally {
        popState()
    }
}

fun Canvas.drawCenteredText(
    textMeasurer: TextMeasurer,
    offset: IntOffset = IntOffset.ZERO,
    text: String,
    color: Color
) {
    val size = textMeasurer.measure(text)
    drawText(offset + size / 2, text, color)
}

fun Canvas.drawCenteredText(textMeasurer: TextMeasurer, offset: IntOffset = IntOffset.ZERO, text: Text, color: Color) {
    val size = textMeasurer.measure(text)
    drawText(offset + size / 2, text, color)
}