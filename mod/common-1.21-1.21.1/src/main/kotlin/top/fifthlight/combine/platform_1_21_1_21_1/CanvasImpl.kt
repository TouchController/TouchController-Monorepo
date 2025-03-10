package top.fifthlight.combine.platform_1_21_1_21_1

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.platform_1_21_x.AbstractCanvasImpl
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.data.Rect
import java.util.function.Supplier

inline fun withShader(program: Supplier<ShaderInstance>, crossinline block: () -> Unit) {
    val originalShader = RenderSystem.getShader()
    RenderSystem.setShader(program)
    block()
    originalShader?.let {
        RenderSystem.setShader { originalShader }
    }
}

class CanvasImpl(drawContext: GuiGraphics) : AbstractCanvasImpl(drawContext) {
    override fun drawTexture(
        identifier: ResourceLocation,
        dstRect: Rect,
        uvRect: Rect,
        tint: Color,
    ) {
        drawContext.flush()
        if (blendEnabled) {
            enableBlend()
        } else {
            disableBlend()
        }
        RenderSystem.setShaderTexture(0, identifier)
        withShader({ GameRenderer.getPositionTexColorShader()!! }) {
            val matrix = drawContext.pose().last().pose()
            val bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            bufferBuilder
                .addVertex(matrix, dstRect.left, dstRect.top, 0f)
                .setUv(uvRect.left, uvRect.top)
                .setColor(tint.value)
            bufferBuilder
                .addVertex(matrix, dstRect.left, dstRect.bottom, 0f)
                .setUv(uvRect.left, uvRect.bottom)
                .setColor(tint.value)
            bufferBuilder
                .addVertex(matrix, dstRect.right, dstRect.bottom, 0f)
                .setUv(uvRect.right, uvRect.bottom)
                .setColor(tint.value)
            bufferBuilder
                .addVertex(matrix, dstRect.right, dstRect.top, 0f)
                .setUv(uvRect.right, uvRect.top)
                .setColor(tint.value)
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
    }

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