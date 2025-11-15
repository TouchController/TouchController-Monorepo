package top.fifthlight.armorstand.ui.component

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.*
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.minecraft.util.Colors
import java.util.function.Consumer

class PagingWidget(
    private val textRenderer: TextRenderer,
    height: Int = 20,
    var currentPage: Int = 1,
    var totalPages: Int,
    val onPrevPage: () -> Unit,
    val onNextPage: () -> Unit,
) : AbstractParentElement(), Widget, Drawable, Selectable, ResizableLayout {
    private var _x = 0
    private var _y = 0
    private var _width = 0
    private var _height = height
    private val buttonHeight = 20
    private val prevPageButton = ButtonWidget.builder(Text.literal("<")) {
        onPrevPage()
    }.apply {
        size(64, buttonHeight)
    }.build()
    private val nextPageButton = ButtonWidget.builder(Text.literal(">")) {
        onNextPage()
    }.apply {
        size(64, buttonHeight)
    }.build()

    override fun children(): List<Element> = listOf(prevPageButton, nextPageButton)

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
    }

    override fun getNavigationFocus(): ScreenRect = super<Widget>.getNavigationFocus()

    override fun setFocused(focused: Boolean) {
        super.setFocused(focused)
        this.focused?.let { focusedElement ->
            focusedElement.isFocused = focused
        }
    }

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        prevPageButton.render(context, mouseX, mouseY, deltaTicks)
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.translatable("armorstand.page.switcher", currentPage, totalPages),
            x + width / 2,
            y + (height - textRenderer.fontHeight) / 2,
            Colors.WHITE
        )
        nextPageButton.render(context, mouseX, mouseY, deltaTicks)
    }

    fun refresh() {
        prevPageButton.active = currentPage > 1
        nextPageButton.active = currentPage < totalPages
    }

    fun init() {
        val buttonY = y + (height - buttonHeight) / 2
        prevPageButton.x = x
        prevPageButton.y = buttonY
        nextPageButton.x = x + width - nextPageButton.width
        nextPageButton.y = buttonY
    }

    override fun getType(): Selectable.SelectionType = maxOf(prevPageButton.type, prevPageButton.type)

    override fun appendNarrations(builder: NarrationMessageBuilder) {
        prevPageButton.appendNarrations(builder)
        nextPageButton.appendNarrations(builder)
    }

    override fun setDimensions(width: Int, height: Int) {
        _width = width
        _height = height
    }

    override fun setX(x: Int) {
        _x = x
    }

    override fun setY(y: Int) {
        _y = y
    }

    override fun getX() = _x

    override fun getY() = _y

    override fun getWidth() = _width

    override fun getHeight() = _height

    override fun forEachChild(consumer: Consumer<ClickableWidget>) {}
}