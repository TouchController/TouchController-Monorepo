package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.tab.Tab
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.LayoutWidget
import net.minecraft.text.Text
import java.util.function.Consumer

class LayoutScreenTab<T>(
    private val title: Text,
    val padding: Insets = Insets.ZERO,
    private val layoutFactory: () -> T,
) : Tab where T : ResizableLayout, T : LayoutWidget {
    override fun getTitle(): Text = this.title

    override fun getNarratedHint(): Text = Text.empty()

    val layout by lazy {
        layoutFactory()
    }

    override fun forEachChild(consumer: Consumer<ClickableWidget>) = layout.forEachChild(consumer)

    override fun refreshGrid(tabArea: ScreenRect) {
        layout.setPosition(
            tabArea.left + padding.left,
            tabArea.top + padding.top,
        )
        layout.setDimensions(
            width = tabArea.width - padding.left - padding.right,
            height = tabArea.height - padding.top - padding.bottom,
        )
        layout.refreshPositions()
    }
}