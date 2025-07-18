package top.fifthlight.combine.platform_1_21_6_1_21_8

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix3x2f
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.platform_1_21_x.AbstractCanvasImpl
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.common_1_21_6_1_21_8.helper.GuiElementUtil
import top.fifthlight.combine.data.Text as CombineText

class CanvasImpl(
    drawContext: GuiGraphics,
) : AbstractCanvasImpl(drawContext) {
    private fun GuiGraphics.submitElement(guiElementRenderState: GuiElementRenderState) =
        (this as SubmittableDrawContext).`touchcontroller$submitElement`(guiElementRenderState)

    private fun GuiGraphics.peekScissorStack() =
        (this as SubmittableDrawContext).`touchcontroller$peekScissorStack`()

    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        drawContext.drawWordWrap(textRenderer, Component.literal(text), offset.x, offset.y, width, color.value, false)
    }

    override fun drawText(
        offset: IntOffset,
        text: CombineText,
        color: Color,
    ) {
        drawContext.drawString(textRenderer, text.toMinecraft(), offset.x, offset.y, color.value, false)
    }

    override fun drawText(
        offset: IntOffset,
        text: String,
        color: Color,
    ) {
        drawContext.drawString(textRenderer, Component.literal(text), offset.x, offset.y, color.value, false)
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        drawContext.drawWordWrap(textRenderer, text.toMinecraft(), offset.x, offset.y, width, color.value, false)
    }

    private data class BlitRenderState(
        val pipeline: RenderPipeline,
        val textureSetup: TextureSetup,
        val pose: Matrix3x2f,
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val u0: Float,
        val u1: Float,
        val v0: Float,
        val v1: Float,
        val color: Int,
        val scissorArea: ScreenRectangle?,
        val bounds: ScreenRectangle?,
    ) : GuiElementRenderState {
        constructor(
            pipeline: RenderPipeline,
            textureSetup: TextureSetup,
            pose: Matrix3x2f,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            u0: Float,
            u1: Float,
            v0: Float,
            v1: Float,
            color: Int,
            screenRectangle: ScreenRectangle?,
        ) : this(
            pipeline = pipeline,
            textureSetup = textureSetup,
            pose = pose,
            x0 = x0,
            y0 = y0,
            x1 = x1,
            y1 = y1,
            u0 = u0,
            u1 = u1,
            v0 = v0,
            v1 = v1,
            color = color,
            scissorArea = screenRectangle,
            bounds = GuiElementUtil.getBounds(x0, y0, x1, y1, pose, screenRectangle)
        )

        override fun buildVertices(vertexConsumer: VertexConsumer, traverseRange: Float) {
            vertexConsumer.addVertexWith2DPose(pose, x0, y0, traverseRange).setUv(u0, v0).setColor(color)
            vertexConsumer.addVertexWith2DPose(pose, x0, y1, traverseRange).setUv(u0, v1).setColor(color)
            vertexConsumer.addVertexWith2DPose(pose, x1, y1, traverseRange).setUv(u1, v1).setColor(color)
            vertexConsumer.addVertexWith2DPose(pose, x1, y0, traverseRange).setUv(u1, v0).setColor(color)
        }

        override fun pipeline() = pipeline

        override fun textureSetup() = textureSetup

        override fun scissorArea() = scissorArea

        override fun bounds() = bounds
    }

    override fun drawTexture(
        identifier: ResourceLocation,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color,
    ) {
        val gpuTextureView = client.textureManager.getTexture(identifier).getTextureView()
        drawContext.submitElement(
            BlitRenderState(
                pipeline = RenderPipelines.GUI_TEXTURED,
                textureSetup = TextureSetup.singleTexture(gpuTextureView),
                pose = Matrix3x2f(drawContext.pose()),
                x0 = dstRect.left,
                y0 = dstRect.top,
                x1 = dstRect.right,
                y1 = dstRect.bottom,
                u0 = uvRect.left,
                u1 = uvRect.right,
                v0 = uvRect.top,
                v1 = uvRect.bottom,
                color = tint.value,
                screenRectangle = drawContext.peekScissorStack(),
            )
        )
    }

    override fun drawItemStack(offset: IntOffset, size: IntSize, stack: ItemStack) {
        val minecraftStack = ((stack as? ItemStackImpl) ?: return).inner
        pushState()
        drawContext.pose().scale(size.width.toFloat() / 16f, size.height.toFloat() / 16f)
        drawContext.renderItem(minecraftStack, offset.x, offset.y)
        popState()
    }

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        drawContext.enableScissor(relativeArea.left, relativeArea.top, relativeArea.right, relativeArea.bottom)
    }

    override fun pushState() {
        drawContext.pose().pushMatrix()
    }

    override fun popState() {
        drawContext.pose().popMatrix()
    }

    override fun translate(x: Int, y: Int) {
        drawContext.pose().translate(x.toFloat(), y.toFloat())
    }

    override fun translate(x: Float, y: Float) {
        drawContext.pose().translate(x, y)
    }

    override fun rotate(degrees: Float) {
        drawContext.pose().rotate(Math.toRadians(degrees.toDouble()).toFloat())
    }

    override fun scale(x: Float, y: Float) {
        drawContext.pose().scale(x, y)
    }

    private data class GradientRectangleRenderState(
        val pipeline: RenderPipeline,
        val textureSetup: TextureSetup,
        val pose: Matrix3x2f,
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val leftTopColor: Int,
        val leftBottomColor: Int,
        val rightTopColor: Int,
        val rightBottomColor: Int,
        val scissorArea: ScreenRectangle?,
        val bounds: ScreenRectangle?,
    ) : GuiElementRenderState {
        constructor(
            pipeline: RenderPipeline,
            textureSetup: TextureSetup,
            pose: Matrix3x2f,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float,
            leftTopColor: Color,
            leftBottomColor: Color,
            rightTopColor: Color,
            rightBottomColor: Color,
            screenRectangle: ScreenRectangle?,
        ) : this(
            pipeline = pipeline,
            textureSetup = textureSetup,
            pose = pose,
            x0 = x0,
            y0 = y0,
            x1 = x1,
            y1 = y1,
            leftTopColor = leftTopColor.value,
            leftBottomColor = leftBottomColor.value,
            rightTopColor = rightTopColor.value,
            rightBottomColor = rightBottomColor.value,
            scissorArea = screenRectangle,
            bounds = GuiElementUtil.getBounds(x0, y0, x1, y1, pose, screenRectangle)
        )

        override fun buildVertices(vertexConsumer: VertexConsumer, traverseRange: Float) {
            vertexConsumer.addVertexWith2DPose(pose, x0, y0, traverseRange).setColor(leftTopColor)
            vertexConsumer.addVertexWith2DPose(pose, x0, y1, traverseRange).setColor(leftBottomColor)
            vertexConsumer.addVertexWith2DPose(pose, x1, y1, traverseRange).setColor(rightBottomColor)
            vertexConsumer.addVertexWith2DPose(pose, x1, y0, traverseRange).setColor(rightTopColor)
        }

        override fun pipeline() = pipeline

        override fun textureSetup() = textureSetup

        override fun scissorArea() = scissorArea

        override fun bounds() = bounds
    }

    override fun fillGradientRect(
        offset: Offset,
        size: Size,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color,
    ) {
        drawContext.submitElement(
            GradientRectangleRenderState(
                pipeline = RenderPipelines.GUI,
                textureSetup = TextureSetup.noTexture(),
                pose = Matrix3x2f(drawContext.pose()),
                x0 = offset.x,
                y0 = offset.y,
                x1 = offset.x + size.width,
                y1 = offset.y + size.height,
                leftTopColor = leftTopColor,
                leftBottomColor = leftBottomColor,
                rightTopColor = rightTopColor,
                rightBottomColor = rightBottomColor,
                screenRectangle = drawContext.peekScissorStack(),
            )
        )
    }
}