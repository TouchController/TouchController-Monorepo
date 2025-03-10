package top.fifthlight.combine.platform_1_21_4

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.platform_1_21_3_1_21_4.CanvasImpl
import top.fifthlight.combine.platform_1_21_x.toMinecraft
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.combine.data.Text as CombineText

class CanvasImpl(
    drawContext: GuiGraphics,
) : CanvasImpl(drawContext) {
    override fun drawText(offset: IntOffset, width: Int, text: String, color: Color) {
        drawContext.drawWordWrap(textRenderer, Component.literal(text), offset.x, offset.y, width, color.value)
    }

    override fun drawText(offset: IntOffset, width: Int, text: CombineText, color: Color) {
        drawContext.drawWordWrap(textRenderer, text.toMinecraft(), offset.x, offset.y, width, color.value)
    }

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        drawContext.enableScissor(relativeArea.left, relativeArea.top, relativeArea.right, relativeArea.bottom)
    }
}
