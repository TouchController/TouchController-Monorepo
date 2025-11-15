package top.fifthlight.armorstand.ui.component

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.text.Text

class IkList(
    client: MinecraftClient,
    width: Int = 0,
    height: Int = 0,
    x: Int = 0,
    y: Int = 0,
    val onClicked: (Int, Boolean) -> Unit,
) : ElementListWidget<IkList.Entry>(
    client,
    width,
    height,
    y,
    24,
) {
    init {
        this.x = x
    }

    override fun drawHeaderAndFooterSeparators(context: DrawContext) {}

    override fun drawMenuListBackground(context: DrawContext) {}

    override fun getScrollbarX() = right - 6

    override fun getRowLeft() = x

    override fun getRowWidth() = if (overflows()) {
        width - 6
    } else {
        width
    }

    fun setList(list: List<Pair<String?, Boolean>>) {
        for ((index, item) in list.withIndex()) {
            if (index >= entryCount) {
                addEntry(Entry().apply {
                    name = item.first
                    enabled = item.second
                    onEnabledChanged = {
                        onClicked(index, it)
                    }
                })
            } else {
                getEntry(index).apply {
                    name = item.first
                    enabled = item.second
                }
            }
        }
        while (entryCount > list.size) {
            remove(entryCount - 1)
        }
    }

    inner class Entry : ElementListWidget.Entry<Entry>() {
        var name: String? = null
            set(value) {
                field = value
                nameLabel.message = value?.let { Text.literal(value) }
                    ?: Text.translatable("armorstand.animation.unnamed_ik_node")
            }
        var enabled: Boolean = false
            set(value) {
                field = value
                checkbox.checked = value
            }
        var onEnabledChanged: ((Boolean) -> Unit)? = null

        private val nameLabel = TextWidget(Text.empty(), client.textRenderer).alignLeft()
        private val checkbox = CheckBoxButton(enabled) { onEnabledChanged?.invoke(!enabled) }

        private val selectableChildren = listOf(checkbox)
        private val children = listOf(nameLabel, checkbox)
        override fun selectableChildren() = selectableChildren
        override fun children() = children

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
            val gap = 8
            nameLabel.setPosition(x + gap, y + (entryHeight - nameLabel.height) / 2)
            nameLabel.setDimensions(entryWidth - checkbox.width - gap * 3, nameLabel.height)
            checkbox.setPosition(x + entryWidth - checkbox.width - gap, y + (entryHeight - checkbox.height) / 2)
            nameLabel.render(context, mouseX, mouseY, tickProgress)
            checkbox.render(context, mouseX, mouseY, tickProgress)
        }
    }
}
