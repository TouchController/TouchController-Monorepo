package top.fifthlight.combine.platform_1_21_1_21_5

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.data.IntOffset
import top.fifthlight.data.Offset
import top.fifthlight.data.Rect
import top.fifthlight.data.Size
import top.fifthlight.touchcontroller.helper.DrawContextWithBuffer
import top.fifthlight.combine.data.Text as CombineText
import top.fifthlight.combine.platform_1_21_x.AbstractCanvasImpl as AbstractCanvasImpl_1_21_x

abstract class AbstractCanvasImpl(
    drawContext: GuiGraphics,
) : AbstractCanvasImpl_1_21_x(drawContext) {
    private val drawContextWithBuffer = drawContext as DrawContextWithBuffer
    protected val vertexConsumers: MultiBufferSource.BufferSource =
        drawContextWithBuffer.`touchcontroller$getVertexConsumers`()

    override fun pushState() {
        drawContext.pose().pushPose()
    }

    override fun popState() {
        drawContext.pose().popPose()
    }

    override fun translate(x: Int, y: Int) {
        drawContext.pose().translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun translate(x: Float, y: Float) {
        drawContext.pose().translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun scale(x: Float, y: Float) {
        drawContext.pose().scale(x, y, 1f)
    }

    override fun fillGradientRect(
        offset: Offset,
        size: Size,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color,
    ) {
        val matrix = drawContext.pose().last().pose()
        val dstRect = Rect(offset, size)
        val renderLayer = RenderType.gui()
        val vertexConsumer = vertexConsumers.getBuffer(renderLayer)
        vertexConsumer
            .addVertex(matrix, dstRect.left, dstRect.top, 0f)
            .setColor(leftTopColor.value)
        vertexConsumer
            .addVertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .setColor(leftBottomColor.value)
        vertexConsumer
            .addVertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .setColor(rightBottomColor.value)
        vertexConsumer
            .addVertex(matrix, dstRect.right, dstRect.top, 0f)
            .setColor(rightTopColor.value)
    }

    override fun drawText(offset: IntOffset, text: String, color: Color) {
        drawContext.drawString(textRenderer, text, offset.x, offset.y, color.value, false)
    }

    override fun drawText(offset: IntOffset, text: CombineText, color: Color) {
        drawContext.drawString(textRenderer, text.toMinecraft(), offset.x, offset.y, color.value, false)
    }
}
