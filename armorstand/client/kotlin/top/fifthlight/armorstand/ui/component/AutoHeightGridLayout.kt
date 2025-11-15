package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.gui.widget.WrapperWidget
import java.util.function.Consumer

class AutoHeightGridLayout(
    x: Int = 0,
    y: Int = 0,
    width: Int = 0,
    height: Int = 0,
    private val cellWidth: Int,
    private val cellHeightRange: IntRange,
    private val forceAtLeastOneRow: Boolean = true,
    private val verticalGap: Int = 0,
    private val padding: Insets = Insets.ZERO,
) : WrapperWidget(x, y, width, height), ResizableLayout {
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

    private val elements = mutableListOf<Element<*>>()

    override fun forEachElement(consumer: Consumer<Widget>) {
        elements.forEach { consumer.accept(it.widget) }
    }

    private var cellHeight = (cellHeightRange.first + cellHeightRange.last) / 2
    override fun setDimensions(width: Int, height: Int) {
        this.width = width
        this.height = height
        val (_, contentHeight) = contentSize()
        val minRows =
            ((contentHeight + verticalGap) / (cellHeightRange.first + verticalGap)).coerceAtLeast(if (forceAtLeastOneRow) 1 else 0)
        val maxRows = ((contentHeight + verticalGap) / (cellHeightRange.last + verticalGap)).coerceAtLeast(minRows)
        val minRowsSpace = (contentHeight + verticalGap) - (minRows * (cellHeightRange.first + verticalGap))
        val maxRowsSpace = (contentHeight + verticalGap) - (maxRows * (cellHeightRange.last + verticalGap))
        val rows = if (minRowsSpace < maxRowsSpace) {
            minRows
        } else {
            maxRows
        }
        cellHeight = if (rows <= 0) {
            cellHeightRange.first
        } else {
            ((contentHeight - (rows - 1) * verticalGap) / rows).coerceIn(cellHeightRange)
        }
    }

    fun clear() = elements.clear()

    fun <T : Widget> add(
        widget: T,
        positioner: Positioner = Positioner.create(),
        onSizeChanged: (widget: T, width: Int, height: Int) -> Unit,
    ) {
        elements.add(Element(widget, positioner, onSizeChanged))
    }

    fun <T : ClickableWidget> add(widget: T, positioner: Positioner = Positioner.create()) {
        add(widget, positioner) { w, width, height -> w.setDimensions(width, height) }
    }

    fun <T> add(widget: T, positioner: Positioner = Positioner.create())
            where T : ResizableLayout, T : Widget {
        add(widget, positioner) { w, width, height -> w.setDimensions(width, height) }
    }

    fun contentSize() = Pair(
        width - padding.left - padding.right,
        height - padding.top - padding.bottom,
    )

    /**
     * Calculate how many rows and columns of elements this grid can contain
     */
    fun calculateSize(): Pair<Int, Int> {
        val (availableWidth, availableHeight) = contentSize()
        val rows =
            ((availableHeight + verticalGap) / (cellHeight + verticalGap)).takeIf { !forceAtLeastOneRow || it >= 1 }
                ?: 1
        val columns = (availableWidth / cellWidth)
        return Pair(rows, columns)
    }

    override fun refreshPositions() {
        if (elements.isEmpty()) return

        val (availableWidth, availableHeight) = contentSize()
        val effectiveCellHeight = if (forceAtLeastOneRow) {
            cellHeight.coerceAtMost(availableHeight)
        } else {
            cellHeight
        }

        val maxItemsPerRow = (availableWidth / cellWidth).coerceAtLeast(1)
        val horizontalGap = if (maxItemsPerRow > 1) {
            (availableWidth - (maxItemsPerRow * cellWidth)) / (maxItemsPerRow - 1)
        } else {
            0
        }

        var currentX = x + padding.left
        var currentY = y + padding.top
        var currentRow = 0

        elements.forEach { element ->
            if (currentRow >= maxItemsPerRow) {
                currentX = x + padding.left
                currentY += effectiveCellHeight + verticalGap
                currentRow = 0
            }

            element.setSize(cellWidth, effectiveCellHeight)
            element.setX(currentX, currentX + cellWidth)
            element.setY(currentY, currentY + effectiveCellHeight)

            currentX += cellWidth + horizontalGap
            currentRow++
        }

        super.refreshPositions()
    }
}
