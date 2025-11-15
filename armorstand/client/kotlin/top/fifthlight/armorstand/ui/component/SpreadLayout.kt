package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.gui.widget.WrapperWidget
import java.util.function.Consumer

class SpreadLayout(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    var direction: Direction = Direction.HORIZONTAL,
    var gap: Int = 0,
    var padding: Insets = Insets.ZERO,
) : WrapperWidget(x, y, width, height), ResizableLayout {

    enum class Direction {
        HORIZONTAL,
        VERTICAL,
    }

    private class Element<T : Widget>(
        private val inner: T,
        positioner: Positioner,
        var weight: Int = 1,
        private val onSizeChanged: (widget: T, width: Int, height: Int) -> Unit = { _, _, _ -> },
    ) : WrappedElement(inner, positioner) {
        fun setSize(width: Int, height: Int) = onSizeChanged(
            inner,
            width - positioner.marginLeft - positioner.marginRight,
            height - positioner.marginTop - positioner.marginBottom
        )
    }

    private val elements = mutableListOf<Element<*>>()

    override fun forEachElement(consumer: Consumer<Widget>) {
        elements.forEach { consumer.accept(it.widget) }
    }

    override fun setDimensions(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun <T : Widget> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        weight: Int = 1,
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit
    ) {
        elements.add(Element(widget, positioner, weight.coerceAtLeast(1), onSizeChanged))
    }

    fun <T : ClickableWidget> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        weight: Int = 1,
    ) {
        add(widget, positioner, weight) { w, width, height -> w.setDimensions(width, height) }
    }

    fun <T> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        weight: Int = 1,
    ) where T : ResizableLayout, T : Widget {
        add(widget, positioner, weight) { w, width, height -> w.setDimensions(width, height) }
    }

    override fun refreshPositions() {
        val count = elements.size
        if (count == 0) return

        when (direction) {
            Direction.HORIZONTAL -> refreshHorizontal()
            Direction.VERTICAL -> refreshVertical()
        }

        super.refreshPositions()
    }

    private fun refreshHorizontal() {
        val count = elements.size
        val horizontalPadding = padding.left + padding.right
        val availableWidth = width - horizontalPadding - gap * (count - 1)
        val availableHeight = height - padding.top - padding.bottom

        val totalWeight = elements.sumOf { it.weight }
        val baseSize = availableWidth / totalWeight

        var remainingSpace = availableWidth - baseSize * totalWeight
        var currentX = x + padding.left

        elements.forEach { element ->
            var elementWidth = baseSize * element.weight
            if (remainingSpace > 0) {
                elementWidth += 1
                remainingSpace -= 1
            }

            element.setSize(elementWidth, availableHeight)
            element.setX(currentX, currentX + elementWidth)
            element.setY(y + padding.top, y + height - padding.bottom)

            currentX += elementWidth + gap
        }
    }

    private fun refreshVertical() {
        val count = elements.size
        val verticalPadding = padding.top + padding.bottom
        val availableWidth = width - padding.left - padding.right
        val availableHeight = height - verticalPadding - gap * (count - 1)

        val totalWeight = elements.sumOf { it.weight }
        val baseSize = availableHeight / totalWeight

        var remainingSpace = availableHeight - baseSize * totalWeight
        var currentY = y + padding.top

        elements.forEach { element ->
            var elementHeight = baseSize * element.weight
            if (remainingSpace > 0) {
                elementHeight += 1
                remainingSpace -= 1
            }

            element.setSize(availableWidth, elementHeight)
            element.setX(x + padding.left, x + width - padding.right)
            element.setY(currentY, currentY + elementHeight)

            currentY += elementHeight + gap
        }
    }
}
