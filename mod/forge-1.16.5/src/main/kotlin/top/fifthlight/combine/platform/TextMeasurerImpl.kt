package top.fifthlight.combine.platform

import net.minecraft.client.Minecraft
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.data.IntSize

object TextMeasurerImpl : TextMeasurer {
    private val client = Minecraft.getInstance()
    private val textRenderer = client.font

    override fun measure(text: String) = IntSize(
        width = textRenderer.width(text),
        height = textRenderer.lineHeight,
    )

    override fun measure(text: String, maxWidth: Int) = IntSize(
        width = textRenderer.width(text).coerceAtMost(maxWidth),
        height = textRenderer.wordWrapHeight(text, maxWidth)
    )

    override fun measure(text: Text) = IntSize(
        width = textRenderer.width(text.toMinecraft()),
        height = textRenderer.lineHeight,
    )

    override fun measure(text: Text, maxWidth: Int): IntSize {
        val inner = text.toMinecraft()
        return IntSize(
            width = textRenderer.width(inner).coerceAtMost(maxWidth),
            height = textRenderer.wordWrapHeight(inner.string, maxWidth)
        )
    }
}