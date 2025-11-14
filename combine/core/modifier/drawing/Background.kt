package top.fifthlight.combine.modifier.drawing

import top.fifthlight.combine.layout.measure.Placeable
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.paint.*
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect

fun Modifier.background(drawable: Drawable) = then(DrawableBackgroundNode(drawable))

fun Modifier.background(color: Color) = background(ColorDrawable(color))

fun Modifier.background(texture: BackgroundTexture, scale: Float = 1f) =
    background(BackgroundTextureDrawable(texture, scale))

private data class DrawableBackgroundNode(
    val drawable: Drawable
) : DrawModifierNode, Modifier.Node<DrawableBackgroundNode> {
    override fun Canvas.renderBefore(node: Placeable) {
        drawable.run { draw(IntRect(offset = IntOffset.ZERO, size = node.size)) }
    }
}
