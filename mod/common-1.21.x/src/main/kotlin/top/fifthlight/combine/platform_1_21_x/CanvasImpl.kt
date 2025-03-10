package top.fifthlight.combine.platform_1_21_x

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.joml.Quaternionf
import top.fifthlight.combine.data.BackgroundTexture
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.paint.*
import top.fifthlight.data.*
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.helper.DrawContextWithBuffer
import top.fifthlight.combine.data.Identifier as CombineIdentifier
import top.fifthlight.combine.data.Text as CombineText

abstract class AbstractCanvasImpl(
    val drawContext: GuiGraphics,
) : Canvas {
    companion object {
        private val IDENTIFIER_ATLAS =
            ResourceLocation.fromNamespaceAndPath("touchcontroller", "textures/gui/atlas.png")
    }

    init {
        enableBlend()
    }

    private val client = Minecraft.getInstance()
    protected val textRenderer = client.font
    override val textLineHeight: Int = textRenderer.lineHeight
    override var blendEnabled = true
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

    override fun rotate(degrees: Float) {
        Quaternionf().apply {
            rotateZ(Math.toRadians(degrees.toDouble()).toFloat())
            drawContext.pose().mulPose(this)
        }
    }

    override fun scale(x: Float, y: Float) {
        drawContext.pose().scale(x, y, 1f)
    }

    override fun fillRect(offset: IntOffset, size: IntSize, color: Color) {
        drawContext.fill(offset.x, offset.y, offset.x + size.width, offset.y + size.height, color.value)
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

    override fun drawRect(offset: IntOffset, size: IntSize, color: Color) {
        drawContext.renderOutline(offset.x, offset.y, size.width, size.height, color.value)
    }

    override fun drawText(offset: IntOffset, text: String, color: Color) {
        drawContext.drawString(textRenderer, text, offset.x, offset.y, color.value, false)
    }

    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        drawContext.drawWordWrap(textRenderer, Component.literal(text), offset.x, offset.y, width, color.value)
    }

    override fun drawText(offset: IntOffset, text: CombineText, color: Color) {
        drawContext.drawString(textRenderer, text.toMinecraft(), offset.x, offset.y, color.value, false)
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

    override fun drawBackgroundTexture(texture: BackgroundTexture, scale: Float, dstRect: Rect) = drawTexture(
        identifier = texture.identifier.toMinecraft(),
        dstRect = dstRect,
        uvRect = Rect(
            offset = Offset.ZERO,
            size = dstRect.size / texture.size.toSize() / scale,
        ),
    )

    abstract override fun drawItemStack(offset: IntOffset, size: IntSize, stack: ItemStack)

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
                BlendFactor.ONE -> GlStateManager.SourceFactor.ONE
                BlendFactor.ZERO -> GlStateManager.SourceFactor.ZERO
                BlendFactor.SRC_COLOR -> GlStateManager.SourceFactor.SRC_COLOR
                BlendFactor.SRC_ALPHA -> GlStateManager.SourceFactor.SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_ALPHA -> GlStateManager.SourceFactor.ONE_MINUS_SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_COLOR -> GlStateManager.SourceFactor.ONE_MINUS_SRC_COLOR
                BlendFactor.DST_COLOR -> GlStateManager.SourceFactor.DST_COLOR
                BlendFactor.DST_ALPHA -> GlStateManager.SourceFactor.DST_ALPHA
                BlendFactor.ONE_MINUS_DST_ALPHA -> GlStateManager.SourceFactor.ONE_MINUS_DST_ALPHA
                BlendFactor.ONE_MINUS_DST_COLOR -> GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR
            }

        fun BlendFactor.toDstFactor() =
            when (this) {
                BlendFactor.ONE -> GlStateManager.DestFactor.ONE
                BlendFactor.ZERO -> GlStateManager.DestFactor.ZERO
                BlendFactor.SRC_COLOR -> GlStateManager.DestFactor.SRC_COLOR
                BlendFactor.SRC_ALPHA -> GlStateManager.DestFactor.SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_ALPHA -> GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                BlendFactor.ONE_MINUS_SRC_COLOR -> GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR
                BlendFactor.DST_COLOR -> GlStateManager.DestFactor.DST_COLOR
                BlendFactor.DST_ALPHA -> GlStateManager.DestFactor.DST_ALPHA
                BlendFactor.ONE_MINUS_DST_ALPHA -> GlStateManager.DestFactor.ONE_MINUS_DST_ALPHA
                BlendFactor.ONE_MINUS_DST_COLOR -> GlStateManager.DestFactor.ONE_MINUS_DST_COLOR
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

    override fun popClip() {
        drawContext.disableScissor()
    }
}