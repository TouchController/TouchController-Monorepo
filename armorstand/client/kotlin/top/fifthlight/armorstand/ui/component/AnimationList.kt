package top.fifthlight.armorstand.ui.component

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors
import top.fifthlight.armorstand.ui.state.AnimationScreenState

class AnimationList(
    client: MinecraftClient,
    width: Int = 0,
    height: Int = 0,
    x: Int = 0,
    y: Int = 0,
    val onClicked: (AnimationScreenState.AnimationItem) -> Unit,
) : AlwaysSelectedEntryListWidget<AnimationList.Entry>(
    client,
    width,
    height,
    y,
    42,
) {
    override fun drawMenuListBackground(context: DrawContext) = Unit
    override fun drawHeaderAndFooterSeparators(context: DrawContext) = Unit

    init {
        this.x = x
    }

    companion object {
        private const val ENTRY_PADDING = 8
    }

    override fun getScrollbarX() = right - 6

    override fun getRowLeft() = x

    override fun getRowWidth() = width - 9

    override fun drawSelectionHighlight(
        context: DrawContext,
        y: Int,
        entryWidth: Int,
        entryHeight: Int,
        borderColor: Int,
        fillColor: Int,
    ) {
        context.fill(rowLeft, y - 2, rowRight, y + entryHeight + 2, borderColor)
        context.fill(rowLeft + 1, y - 1, rowRight - 1, y + entryHeight + 1, fillColor)
    }

    fun setEntries(entries: List<AnimationScreenState.AnimationItem>) {
        clearEntries()
        for (item in entries) {
            addEntry(Entry(item))
        }
    }

    inner class Entry(val item: AnimationScreenState.AnimationItem) : AlwaysSelectedEntryListWidget.Entry<Entry>() {
        private val name =
            item.name?.let { name -> Text.literal(name) } ?: Text.translatable("armorstand.animation.name.unnamed")
        private val length = item.duration?.let { duration ->
            val minutes = (duration / 60).toInt().toString().padStart(2, '0')
            val seconds = (duration % 60).toInt().toString().padStart(2, '0')
            Text.literal("$minutes:$seconds")
        }
        private val source = when (val source = item.source) {
            is AnimationScreenState.AnimationItem.Source.Embed -> Text.translatable("armorstand.animation.source.embed")
            is AnimationScreenState.AnimationItem.Source.External -> Text.translatable(
                "armorstand.animation.source.external",
                source.path.fileName
            )
        }

        override fun getNarration(): Text = Text.empty()

        override fun render(
            context: DrawContext,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickProgress: Float,
        ) {
            val textRenderer = client.textRenderer
            val length = length
            val timeWidth = length?.let { length -> textRenderer.getWidth(length) }
            val nameTextWidth = textRenderer.getWidth(name)
            val timeExtraPadding = 2
            val nameAreaWidth = if (timeWidth != null) {
                entryWidth - ENTRY_PADDING * 3 - timeWidth - timeExtraPadding
            } else {
                entryWidth - ENTRY_PADDING * 2
            }
            drawScrollableText(
                context,
                textRenderer,
                name,
                x + ENTRY_PADDING + nameTextWidth / 2,
                x + ENTRY_PADDING,
                y + ENTRY_PADDING,
                x + ENTRY_PADDING + nameAreaWidth,
                y + ENTRY_PADDING + textRenderer.fontHeight,
                Colors.WHITE,
            )
            length?.let { length ->
                context.drawTextWithShadow(
                    textRenderer,
                    length,
                    x + entryWidth - ENTRY_PADDING - timeWidth!! - timeExtraPadding,
                    y + ENTRY_PADDING,
                    Colors.GRAY,
                )
            }
            val sourceTextWidth = textRenderer.getWidth(source)
            drawScrollableText(
                context,
                textRenderer,
                source,
                x + ENTRY_PADDING + sourceTextWidth / 2,
                x + ENTRY_PADDING,
                y + entryHeight - ENTRY_PADDING - textRenderer.fontHeight,
                x + entryWidth - ENTRY_PADDING,
                y + entryHeight - ENTRY_PADDING,
                Colors.GRAY,
            )
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            onClicked(item)
            return true
        }
    }
}