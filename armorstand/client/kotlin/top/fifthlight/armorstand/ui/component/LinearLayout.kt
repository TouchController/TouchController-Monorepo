package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.gui.widget.WrapperWidget
import java.util.function.Consumer

class LinearLayout(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    var direction: Direction = Direction.HORIZONTAL,
    val align: Align = Align.START,
    var gap: Int = 0,
    var padding: Insets = Insets.ZERO,
    val surface: Surface = Surface.empty,
) : WrapperWidget(x, y, width, height), Drawable, ResizableLayout {
    enum class Direction {
        HORIZONTAL,
        VERTICAL,
    }

    enum class Align {
        START,
        CENTER,
        END,
    }

    private class Element<T : Widget>(
        private val inner: T,
        positioner: Positioner,
        private val onSizeChanged: (widget: T, width: Int, height: Int) -> Unit,
    ) : WrappedElement(inner, positioner) {
        fun setSize(width: Int, height: Int) = onSizeChanged(
            inner,
            width - positioner.marginLeft - positioner.marginRight,
            height - positioner.marginTop - positioner.marginBottom
        )
    }

    private val elements = mutableListOf<Element<*>>()

    fun <T : Widget> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit,
    ) {
        elements.add(Element(widget, positioner, onSizeChanged))
    }

    fun <T : ClickableWidget> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        expand: Boolean = false,
    ) {
        if (expand) {
            add(widget, positioner) { w, width, height -> w.setDimensions(width, height) }
        } else {
            add(widget, positioner) { w, width, height -> }
        }
    }

    fun <T> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        expand: Boolean = false,
    ) where T : ResizableLayout, T : Widget {
        if (expand) {
            add(widget, positioner) { w, width, height -> w.setDimensions(width, height) }
        } else {
            add(widget, positioner) { w, width, height -> }
        }
    }

    fun removeAt(index: Int): Widget = elements.removeAt(index).widget

    fun <T : Widget> setAt(
        index: Int,
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit,
    ) {
        elements[index] = Element(widget, positioner, onSizeChanged)
    }

    fun <T : ClickableWidget> setAt(
        index: Int,
        widget: T,
        positioner: Positioner = Positioner.create(),
        expand: Boolean = false,
    ) {
        if (expand) {
            setAt(index, widget, positioner) { w, width, height -> w.setDimensions(width, height) }
        } else {
            setAt(index, widget, positioner) { w, width, height -> }
        }
    }

    fun <T> setAt(
        index: Int,
        widget: T,
        positioner: Positioner = Positioner.create(),
        expand: Boolean = false,
    ) where T : ResizableLayout, T : Widget {
        if (expand) {
            setAt(index, widget, positioner) { w, width, height -> w.setDimensions(width, height) }
        } else {
            setAt(index, widget, positioner) { w, width, height -> }
        }
    }

    fun clear() = elements.map { it.widget }.also { elements.clear() }

    override fun setDimensions(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun pack() = when (direction) {
        Direction.HORIZONTAL -> {
            width = padding.left + padding.right + elements.sumOf { it.width } + gap * (elements.size - 1)
        }
        Direction.VERTICAL -> {
            height = padding.top + padding.bottom + elements.sumOf { it.height } + gap * (elements.size - 1)
        }
    }

    override fun refreshPositions() {
        val sizes = elements.map {
            when (direction) {
                Direction.HORIZONTAL -> it.width
                Direction.VERTICAL -> it.height
            }
        }
        val totalSize = sizes.sum() + gap * (sizes.size - 1)
        val initial = when (direction) {
            Direction.HORIZONTAL -> this@LinearLayout.x + padding.left
            Direction.VERTICAL -> this@LinearLayout.y + padding.top
        }
        val total = when (direction) {
            Direction.HORIZONTAL -> width - padding.left - padding.right
            Direction.VERTICAL -> height - padding.top - padding.bottom
        }
        var pos = initial + when (align) {
            Align.START -> 0
            Align.CENTER -> (total - totalSize) / 2
            Align.END -> total - totalSize
        }
        for ((index, size) in sizes.withIndex()) {
            val element = elements[index]
            when (direction) {
                Direction.HORIZONTAL -> {
                    val availableHeight = height - padding.top - padding.bottom
                    element.setSize(element.width, availableHeight)
                    element.setX(pos, pos + element.width)
                    element.setY(this@LinearLayout.y + padding.top, availableHeight)
                }

                Direction.VERTICAL -> {
                    val availableWidth = width - padding.left - padding.right
                    element.setSize(availableWidth, element.height)
                    element.setX(this@LinearLayout.x + padding.left, availableWidth)
                    element.setY(pos, pos + element.height)
                }
            }
            pos += size + gap
        }
        super.refreshPositions()
    }

    override fun forEachElement(consumer: Consumer<Widget>) {
        elements.forEach { consumer.accept(it.widget) }
    }

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        surface.draw(context, this@LinearLayout.x, this@LinearLayout.y, width, height)
    }
}