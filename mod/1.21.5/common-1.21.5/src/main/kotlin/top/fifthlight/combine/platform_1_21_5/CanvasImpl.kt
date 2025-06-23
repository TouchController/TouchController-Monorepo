package top.fifthlight.combine.platform_1_21_5

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.joml.Quaternionf
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.platform_1_21_3_1_21_5.ItemStackImpl
import top.fifthlight.combine.platform_1_21_1_21_5.AbstractCanvasImpl
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.data.Rect
import top.fifthlight.combine.data.Text as CombineText

open class CanvasImpl(
    drawContext: GuiGraphics,
) : AbstractCanvasImpl(drawContext) {
    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        drawContext.drawWordWrap(textRenderer, Component.literal(text), offset.x, offset.y, width, color.value, false)
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        drawContext.drawWordWrap(textRenderer, text.toMinecraft(), offset.x, offset.y, width, color.value, false)
    }

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

    override fun drawItemStack(offset: IntOffset, size: IntSize, stack: ItemStack) {
        val minecraftStack = ((stack as? ItemStackImpl) ?: return).inner
        pushState()
        drawContext.pose().scale(size.width.toFloat() / 16f, size.height.toFloat() / 16f, 1f)
        drawContext.renderItem(minecraftStack, offset.x, offset.y)
        popState()
    }

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        drawContext.enableScissor(relativeArea.left, relativeArea.top, relativeArea.right, relativeArea.bottom)
    }

    override fun rotate(degrees: Float) {
        Quaternionf().apply {
            rotateZ(Math.toRadians(degrees.toDouble()).toFloat())
            drawContext.pose().mulPose(this)
        }
    }
}