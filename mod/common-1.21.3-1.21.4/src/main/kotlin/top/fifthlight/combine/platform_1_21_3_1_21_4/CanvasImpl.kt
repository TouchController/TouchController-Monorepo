package top.fifthlight.combine.platform_1_21_3_1_21_4

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.platform_1_21_x.AbstractCanvasImpl
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.data.Rect
import top.fifthlight.combine.data.Identifier as CombineIdentifier

open class CanvasImpl(
    drawContext: GuiGraphics,
) : AbstractCanvasImpl(drawContext) {
    override fun drawTexture(
        identifier: ResourceLocation,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color,
    ) {
        val renderLayer = RenderType.guiTextured(identifier)
        val matrix = drawContext.pose().last().pose()
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        vertexConsumer
            .addVertex(matrix, dstRect.left, dstRect.top, 0f)
            .setUv(uvRect.left, uvRect.top)
            .setColor(tint.value)
        vertexConsumer
            .addVertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .setUv(uvRect.left, uvRect.bottom)
            .setColor(tint.value)
        vertexConsumer
            .addVertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .setUv(uvRect.right, uvRect.bottom)
            .setColor(tint.value)
        vertexConsumer
            .addVertex(matrix, dstRect.right, dstRect.top, 0f)
            .setUv(uvRect.right, uvRect.top)
            .setColor(tint.value)
    }

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

    override fun drawItemStack(offset: IntOffset, size: IntSize, stack: ItemStack) {
        val minecraftStack = ((stack as? ItemStackImpl) ?: return).inner
        pushState()
        drawContext.pose().scale(size.width.toFloat() / 16f, size.height.toFloat() / 16f, 1f)
        drawContext.renderItem(minecraftStack, offset.x, offset.y)
        popState()
    }

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        drawContext.enableScissor(absoluteArea.left, absoluteArea.top, absoluteArea.right, absoluteArea.bottom)
    }
}