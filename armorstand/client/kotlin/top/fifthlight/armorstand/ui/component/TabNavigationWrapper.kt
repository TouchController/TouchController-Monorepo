package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.*
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.world.CreateWorldScreen
import net.minecraft.client.gui.tab.TabManager
import net.minecraft.client.gui.widget.LayoutWidget
import net.minecraft.client.gui.widget.TabNavigationWidget
import net.minecraft.client.gui.widget.Widget
import java.util.function.Consumer

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
class TabNavigationWrapper(
    val tabManager: TabManager,
    val inner: TabNavigationWidget,
    val surface: Surface = Surface.empty,
) : ParentElement by inner, Selectable by inner, Widget, ResizableLayout, Drawable, LayoutWidget {
    private var height: Int = 0
    private var width: Int = 0
    private var _x = 0
    private var _y = 0

    override fun getX() = _x

    override fun getY() = _y

    override fun setX(x: Int) {
        this._x = x
    }

    override fun setY(y: Int) {
        this._y = y
    }

    override fun setPosition(x: Int, y: Int) {
        this._x = x
        this._y = y
    }

    override fun setDimensions(width: Int, height: Int) {
        inner.setWidth(width)
        this.height = height
        this.width = width
    }

    override fun getWidth() = width

    override fun getHeight() = height

    override fun forEachElement(consumer: Consumer<Widget>) {}

    override fun isMouseOver(mouseX: Double, mouseY: Double) = inner.isMouseOver(mouseX, mouseY)

    override fun refreshPositions() {
        inner.init()
        val realWidth = width.coerceAtMost(400) - 28
        inner.grid.setPosition((width - realWidth) / 2 + _x, _y)
        inner.grid.refreshPositions()
        val navHeight = inner.navigationFocus.height()
        val area = ScreenRect(_x, _y + navHeight, width, height - navHeight)
        tabManager.setTabArea(area)
    }

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            CreateWorldScreen.TAB_HEADER_BACKGROUND_TEXTURE,
            _x,
            _y,
            0.0F,
            0.0F,
            width,
            inner.navigationFocus.height(),
            16,
            16,
        )
        val left = inner.tabButtons.first().x
        val right = inner.tabButtons.last().right
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            Screen.HEADER_SEPARATOR_TEXTURE,
            _x,
            _y + inner.grid.height - 2,
            0.0f,
            0.0f,
            left - _x,
            2,
            32,
            2,
        )
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            Screen.HEADER_SEPARATOR_TEXTURE,
            right,
            _y + inner.grid.height - 2,
            0.0F,
            0.0F,
            _x + width - right,
            2,
            32,
            2,
        )
        inner.tabButtons.forEach { button ->
            button.render(context, mouseX, mouseY, deltaTicks)
        }
        surface.draw(context, _x, _y + inner.grid.height, width, height - inner.grid.height)
    }

    override fun isNarratable() = inner.isNarratable

    override fun getNarratedParts(): Collection<Selectable> = inner.narratedParts

    override fun getNavigationOrder() = inner.navigationOrder

    override fun getNavigationFocus(): ScreenRect = inner.navigationFocus
}