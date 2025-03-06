package top.fifthlight.combine.platform

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.Quaternion
import net.minecraft.util.math.Vec3f
import org.lwjgl.opengl.GL11
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.*
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.mixin.Matrix4fAccessor
import top.fifthlight.combine.data.Text as CombineText

fun VertexConsumer.color(color: Color): VertexConsumer = color(color.r, color.g, color.b, color.a)

class CanvasImpl(
    val matrices: MatrixStack,
) : Canvas, DrawableHelper() {
    companion object {
        private val IDENTIFIER_ATLAS = Identifier("touchcontroller", "textures/gui/atlas.png")
    }

    init {
        enableBlend()
    }

    private val client = MinecraftClient.getInstance()
    private val textRenderer = client.textRenderer
    override val textLineHeight: Int = textRenderer.fontHeight
    override var blendEnabled = true

    override fun pushState() {
        matrices.push()
    }

    override fun popState() {
        matrices.pop()
    }

    override fun translate(x: Int, y: Int) {
        matrices.translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun translate(x: Float, y: Float) {
        matrices.translate(x.toDouble(), y.toDouble(), 0.0)
    }

    override fun rotate(degrees: Float) {
        matrices.multiply(Quaternion(Vec3f.POSITIVE_Z, degrees, true))
    }

    override fun scale(x: Float, y: Float) {
        matrices.scale(x, y, 1f)
    }

    override fun fillRect(offset: IntOffset, size: IntSize, color: Color) {
        fill(matrices, offset.x, offset.y, offset.x + size.width, offset.y + size.height, color.value)
        if (blendEnabled) {
            enableBlend()
        } else {
            disableBlend()
        }
    }

    @Suppress("DEPRECATION")
    override fun fillGradientRect(
        offset: Offset,
        size: Size,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color
    ) {
        RenderSystem.disableAlphaTest()
        RenderSystem.disableTexture()
        RenderSystem.shadeModel(GL11.GL_SMOOTH)
        val matrix = matrices.peek().model
        val bufferBuilder = Tessellator.getInstance().buffer
        bufferBuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR)
        val dstRect = Rect(offset, size)
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.top, 0f)
            .color(leftTopColor)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .color(leftBottomColor)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .color(rightBottomColor)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.top, 0f)
            .color(rightTopColor)
            .next()
        bufferBuilder.end()
        BufferRenderer.draw(bufferBuilder)
        RenderSystem.shadeModel(GL11.GL_FLAT)
        RenderSystem.enableTexture()
        RenderSystem.enableAlphaTest()
    }

    override fun drawRect(offset: IntOffset, size: IntSize, color: Color) {
        //  1 -> 2  |
        //  |    |  |
        //  4 -> 3 \|/

        val strokeSize = 1

        // 1 to 2
        fillRect(
            offset = offset,
            size = IntSize(
                width = size.width - strokeSize,
                height = strokeSize,
            ),
            color = color,
        )

        // 2 to 3
        fillRect(
            offset = IntOffset(
                x = offset.x + size.width - strokeSize,
                y = offset.y,
            ),
            size = IntSize(
                width = strokeSize,
                height = size.height - strokeSize,
            ),
            color = color,
        )

        // 4 to 3
        fillRect(
            offset = IntOffset(
                x = offset.x + strokeSize,
                y = offset.y + size.height - strokeSize,
            ),
            size = IntSize(
                width = size.width - strokeSize,
                height = strokeSize,
            ),
            color = color,
        )

        // 4 to 1
        fillRect(
            offset = IntOffset(
                x = offset.x,
                y = offset.y + strokeSize,
            ),
            size = IntSize(
                width = strokeSize,
                height = size.height - strokeSize,
            ),
            color = color,
        )
    }

    override fun drawText(offset: IntOffset, text: String, color: Color) {
        textRenderer.draw(matrices, text, offset.x.toFloat(), offset.y.toFloat(), color.value)
    }

    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        var y = offset.y.toFloat()
        for (line in textRenderer.wrapLines(Text.of(text), width)) {
            textRenderer.draw(matrices, line, offset.x.toFloat(), y, color.value)
            y += textRenderer.fontHeight
        }
    }

    override fun drawText(offset: IntOffset, text: CombineText, color: Color) {
        textRenderer.draw(matrices, text.toMinecraft(), offset.x.toFloat(), offset.y.toFloat(), color.value)
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        var y = offset.y.toFloat()
        for (line in textRenderer.wrapLines(text.toMinecraft(), width)) {
            textRenderer.draw(matrices, line, offset.x.toFloat(), y, color.value)
            y += textRenderer.fontHeight
        }
    }

    private fun drawTexture(
        identifier: Identifier,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color = Colors.WHITE,
    ) {
        if (blendEnabled) {
            enableBlend()
        } else {
            disableBlend()
        }
        this.client.textureManager.bindTexture(identifier)
        val matrix = matrices.peek().model
        val bufferBuilder = Tessellator.getInstance().buffer
        bufferBuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE)
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.top, 0f)
            .color(tint)
            .texture(uvRect.left, uvRect.top)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.left, dstRect.bottom, 0f)
            .color(tint)
            .texture(uvRect.left, uvRect.bottom)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.bottom, 0f)
            .color(tint)
            .texture(uvRect.right, uvRect.bottom)
            .next()
        bufferBuilder
            .vertex(matrix, dstRect.right, dstRect.top, 0f)
            .color(tint)
            .texture(uvRect.right, uvRect.top)
            .next()
        bufferBuilder.end()
        BufferRenderer.draw(bufferBuilder)
    }

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

    override fun drawBackgroundTexture(texture: BackgroundTexture, scale: Float, dstRect: Rect) = drawTexture(
        identifier = texture.identifier.toMinecraft(),
        dstRect = dstRect,
        uvRect = Rect(
            offset = Offset.ZERO,
            size = dstRect.size / texture.size.toSize() / scale,
        ),
    )

    override fun drawItemStack(offset: IntOffset, size: IntSize, stack: ItemStack) {
        val matrix = matrices.peek().model
        // TODO apply matrix when rendering
        @Suppress("CAST_NEVER_SUCCEEDS")
        val accessor = matrix as Matrix4fAccessor
        client.itemRenderer.renderGuiItemIcon(
            stack.toVanilla(),
            accessor.a03.toInt() + offset.left,
            accessor.a13.toInt() + offset.top
        )
        if (blendEnabled) {
            enableBlend()
        } else {
            disableBlend()
        }
    }

    override fun enableBlend() {
        blendEnabled = true
        RenderSystem.enableBlend()
    }

    override fun disableBlend() {
        blendEnabled = false
        RenderSystem.disableBlend()
    }

    override fun blendFunction(func: BlendFunction) {
        fun BlendFactor.toSrcFactor() =
            when (this) {
                BlendFactor.ONE -> GlStateManager.SrcFactor.ONE
                BlendFactor.ZERO -> GlStateManager.SrcFactor.ZERO
                BlendFactor.SRC_COLOR -> GlStateManager.SrcFactor.SRC_COLOR
                BlendFactor.SRC_ALPHA -> GlStateManager.SrcFactor.SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_ALPHA -> GlStateManager.SrcFactor.ONE_MINUS_SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_COLOR -> GlStateManager.SrcFactor.ONE_MINUS_SRC_COLOR
                BlendFactor.DST_COLOR -> GlStateManager.SrcFactor.DST_COLOR
                BlendFactor.DST_ALPHA -> GlStateManager.SrcFactor.DST_ALPHA
                BlendFactor.ONE_MINUS_DST_ALPHA -> GlStateManager.SrcFactor.ONE_MINUS_DST_ALPHA
                BlendFactor.ONE_MINUS_DST_COLOR -> GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR
            }

        fun BlendFactor.toDstFactor() =
            when (this) {
                BlendFactor.ONE -> GlStateManager.DstFactor.ONE
                BlendFactor.ZERO -> GlStateManager.DstFactor.ZERO
                BlendFactor.SRC_COLOR -> GlStateManager.DstFactor.SRC_COLOR
                BlendFactor.SRC_ALPHA -> GlStateManager.DstFactor.SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_ALPHA -> GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_COLOR -> GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR
                BlendFactor.DST_COLOR -> GlStateManager.DstFactor.DST_COLOR
                BlendFactor.DST_ALPHA -> GlStateManager.DstFactor.DST_ALPHA
                BlendFactor.ONE_MINUS_DST_ALPHA -> GlStateManager.DstFactor.ONE_MINUS_DST_ALPHA
                BlendFactor.ONE_MINUS_DST_COLOR -> GlStateManager.DstFactor.ONE_MINUS_DST_COLOR
            }

        RenderSystem.blendFuncSeparate(
            func.srcFactor.toSrcFactor(),
            func.dstFactor.toDstFactor(),
            func.srcAlpha.toSrcFactor(),
            func.dstAlpha.toDstFactor()
        )
    }

    override fun defaultBlendFunction() {
        RenderSystem.defaultBlendFunc()
    }

    private val clipStack = arrayListOf<IntRect>()

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        val scaleFactor = client.window.scaleFactor.toInt()
        val rect = IntRect(
            offset = absoluteArea.offset * scaleFactor,
            size = absoluteArea.size * scaleFactor,
        )
        RenderSystem.enableScissor(
            rect.left,
            client.window.framebufferHeight - rect.bottom,
            rect.size.width,
            rect.size.height
        )
        clipStack.add(rect)
    }

    override fun popClip() {
        if (clipStack.isEmpty()) {
            return
        } else if (clipStack.size == 1) {
            clipStack.clear()
            RenderSystem.disableScissor()
        } else {
            val item = clipStack.removeLast<IntRect>()
            RenderSystem.enableScissor(item.left, client.window.height - item.bottom, item.size.width, item.size.height)
        }
    }
}