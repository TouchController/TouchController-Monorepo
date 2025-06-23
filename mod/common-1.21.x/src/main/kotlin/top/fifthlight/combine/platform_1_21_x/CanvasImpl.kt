package top.fifthlight.combine.platform_1_21_x

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.combine.data.Identifier as CombineIdentifier
import top.fifthlight.combine.data.Text as CombineText

abstract class AbstractCanvasImpl(
    val drawContext: GuiGraphics,
) : Canvas {
    companion object {
        private val IDENTIFIER_ATLAS =
            ResourceLocation.fromNamespaceAndPath("touchcontroller", "textures/gui/atlas.png")
    }

    protected val client: Minecraft = Minecraft.getInstance()
    protected val textRenderer: Font = client.font
    override val textLineHeight: Int = textRenderer.lineHeight

    override fun fillRect(offset: IntOffset, size: IntSize, color: Color) {
        drawContext.fill(offset.x, offset.y, offset.x + size.width, offset.y + size.height, color.value)
    }

    override fun drawRect(offset: IntOffset, size: IntSize, color: Color) {
        drawContext.renderOutline(offset.x, offset.y, size.width, size.height, color.value)
    }

    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        drawContext.drawWordWrap(textRenderer, Component.literal(text), offset.x, offset.y, width, color.value)
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        drawContext.drawWordWrap(textRenderer, text.toMinecraft(), offset.x, offset.y, width, color.value)
    }

    protected abstract fun drawTexture(
        identifier: ResourceLocation,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color = Colors.WHITE,
    )

    override fun drawTexture(
        identifier: CombineIdentifier,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color,
    ) = drawTexture(
        identifier = identifier.toMinecraft(),
        dstRect = dstRect,
        uvRect = uvRect,
        tint = tint,
    )

    override fun drawTexture(
        texture: Texture,
        dstRect: Rect,
        srcRect: IntRect,
        tint: Color,
    ) = drawTexture(
        identifier = IDENTIFIER_ATLAS,
        dstRect = dstRect,
        uvRect = Rect(
            offset = (texture.atlasOffset + srcRect.offset).toOffset() / Textures.atlasSize.toSize(),
            size = srcRect.size.toSize() / Textures.atlasSize.toSize(),
        ),
        tint = tint,
    )

    override fun drawBackgroundTexture(
        texture: BackgroundTexture,
        scale: Float,
        dstRect: Rect,
        tint: Color,
    ) = drawTexture(
        identifier = texture.identifier.toMinecraft(),
        dstRect = dstRect,
        uvRect = Rect(
            offset = Offset.ZERO,
            size = dstRect.size / texture.size.toSize() / scale,
        ),
        tint = tint,
    )

    abstract override fun drawItemStack(offset: IntOffset, size: IntSize, stack: ItemStack)

    override fun popClip() {
        drawContext.disableScissor()
    }
}