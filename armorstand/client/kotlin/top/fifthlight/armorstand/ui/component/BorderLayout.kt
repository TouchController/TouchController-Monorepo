package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.gui.widget.WrapperWidget
import java.util.function.Consumer

class BorderLayout(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    var direction: Direction = Direction.HORIZONTAL,
    val surface: Surface = Surface.empty,
) : WrapperWidget(x, y, width, height), ResizableLayout, Drawable {
    enum class Direction {
        HORIZONTAL,
        VERTICAL
    }

    private class Element<T : Widget>(
        private val inner: T,
        positioner: Positioner,
        private val onSizeChanged: (widget: T, width: Int, height: Int) -> Unit = { _, _, _ -> },
    ) : WrappedElement(inner, positioner) {
        fun setSize(width: Int, height: Int) = onSizeChanged(
            inner,
            width - positioner.marginLeft - positioner.marginRight,
            height - positioner.marginTop - positioner.marginBottom
        )
    }

    private var firstElement: Element<*>? = null
    private var secondElement: Element<*>? = null
    private var centerElement: Element<*>? = null

    override fun forEachElement(consumer: Consumer<Widget>) {
        firstElement?.let { consumer.accept(it.widget) }
        centerElement?.let { consumer.accept(it.widget) }
        secondElement?.let { consumer.accept(it.widget) }
    }

    fun <T : Widget> setFirstElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit,
    ) {
        firstElement = Element(widget, positioner, onSizeChanged)
    }

    fun <T : Widget> setSecondElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit,
    ) {
        secondElement = Element(widget, positioner, onSizeChanged)
    }

    fun <T : Widget> setCenterElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit,
    ) {
        centerElement = Element(widget, positioner, onSizeChanged)
    }


    fun <T : ClickableWidget> setFirstElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
    ) {
        firstElement = Element(widget, positioner) { widget, width, height -> widget.setDimensions(width, height) }
    }

    fun <T : ClickableWidget> setSecondElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
    ) {
        secondElement = Element(widget, positioner) { widget, width, height -> widget.setDimensions(width, height) }
    }

    fun <T : ClickableWidget> setCenterElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
    ) {
        centerElement = Element(widget, positioner) { widget, width, height -> widget.setDimensions(width, height) }
    }

    fun <T> setFirstElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
    ) where T : ResizableLayout, T: Widget {
        firstElement = Element(widget, positioner) { widget, width, height -> widget.setDimensions(width, height) }
    }

    fun <T> setSecondElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
    ) where T : ResizableLayout, T: Widget {
        secondElement = Element(widget, positioner) { widget, width, height -> widget.setDimensions(width, height) }
    }

    fun <T> setCenterElement(
        widget: T,
        positioner: Positioner = Positioner.create(),
    ) where T : ResizableLayout, T: Widget {
        centerElement = Element(widget, positioner) { widget, width, height -> widget.setDimensions(width, height) }
    }

    override fun setDimensions(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun refreshPositions() {
        val first = firstElement
        val second = secondElement
        val center = centerElement

        when (direction) {
            Direction.HORIZONTAL -> {
                val leftWidth = first?.width ?: 0
                val rightWidth = second?.width ?: 0
                val centerWidth = width - leftWidth - rightWidth
                first?.let {
                    it.setSize(leftWidth, height)
                    it.setX(x, x + leftWidth)
                    it.setY(y, y + height)
                }
                center?.let {
                    it.setSize(centerWidth, height)
                    it.setX(x + leftWidth, x + width - rightWidth)
                    it.setY(y, y + height)
                }
                second?.let {
                    it.setSize(rightWidth, height)
                    it.setX(x + width - rightWidth, x + width)
                    it.setY(y, y + height)
                }
            }

            Direction.VERTICAL -> {
                val topHeight = first?.height ?: 0
                val bottomHeight = second?.height ?: 0
                val centerHeight = height - topHeight - bottomHeight
                first?.let {
                    it.setSize(width, topHeight)
                    it.setX(x, width)
                    it.setY(y, topHeight)
                }
                center?.let {
                    it.setSize(width, centerHeight)
                    it.setX(x, width)
                    it.setY(y + topHeight, height - bottomHeight)
                }
                second?.let {
                    it.setSize(width, bottomHeight)
                    it.setX(x, width)
                    it.setY(y + height - bottomHeight, height)
                }
            }
        }
        super.refreshPositions()
    }

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        surface.draw(context, this@BorderLayout.x, this@BorderLayout.y, width, height)
    }
}