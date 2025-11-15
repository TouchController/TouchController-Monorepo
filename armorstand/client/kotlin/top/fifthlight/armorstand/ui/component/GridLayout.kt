package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.gui.widget.WrapperWidget
import java.util.function.Consumer
import kotlin.math.max

class GridLayout(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    private val surface: Surface?,
    private val gridPadding: Insets = Insets.ZERO,
) : WrapperWidget(x, y, width, height), ResizableLayout, Drawable {
    private class Element<T : Widget>(
        val column: Int,
        val row: Int,
        inner: T,
        positioner: Positioner,
    ) : WrappedElement(inner, positioner)

    private val elements = mutableListOf<Element<*>>()
    private val grids = mutableMapOf<Pair<Int, Int>, Element<*>>()

    override fun forEachElement(consumer: Consumer<Widget>) {
        elements.forEach { consumer.accept(it.widget) }
    }

    override fun setDimensions(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    private val rowHeights = LinkedHashMap<Int, Int>()
    private val columnWidths = LinkedHashMap<Int, Int>()

    private fun recalculateSizes() {
        rowHeights.clear()
        columnWidths.clear()
        grids.clear()
        for (element in elements) {
            grids[element.column to element.row] = element
            val rowHeight = rowHeights[element.row]
            val columnWidth = columnWidths[element.column]
            rowHeights[element.row] = max(rowHeight ?: 0, element.widget.height + gridPadding.top + gridPadding.bottom)
            columnWidths[element.column] =
                max(columnWidth ?: 0, element.widget.width + gridPadding.left + gridPadding.right)
        }
    }

    fun <T : Widget> add(
        column: Int,
        row: Int,
        widget: T,
        positioner: Positioner = Positioner.create().alignHorizontalCenter().alignVerticalCenter(),
    ) {
        elements.add(Element(column, row, widget, positioner))
    }

    fun clear() {
        elements.clear()
        grids.clear()
        rowHeights.clear()
        columnWidths.clear()
    }

    fun pack() {
        recalculateSizes()
        val totalRowHeight = rowHeights.values.sum()
        val totalColumnWidth = columnWidths.values.sum()
        setDimensions(totalColumnWidth, totalRowHeight)
    }

    override fun refreshPositions() {
        recalculateSizes()
        var currentY = y
        for (row in rowHeights.sequencedKeySet()) {
            var currentX = x
            val columnHeight = rowHeights[row] ?: 0
            for (column in columnWidths.sequencedKeySet()) {
                val columnWidth = columnWidths[column] ?: 0
                val element = grids[column to row]
                element?.setX(currentX + gridPadding.left, columnWidth - gridPadding.left - gridPadding.right)
                element?.setY(currentY + gridPadding.top, columnHeight - gridPadding.top - gridPadding.bottom)
                currentX += columnWidth
            }
            currentY += columnHeight
        }
        super.refreshPositions()
    }

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        surface?.draw(context, x, y, width, height)
    }
}