package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.LayoutWidget
import net.minecraft.util.Identifier
import java.util.function.Consumer

class LoadingOverlay<T>(
    val inner: T,
    var loading: Boolean = true,
) : LayoutWidget by inner, ResizableLayout by inner, Drawable where T: LayoutWidget, T: ResizableLayout {
    companion object {
        private val LOADING_ICON: Identifier = Identifier.of("armorstand", "loading")
        private const val ICON_WIDTH = 32
        private const val ICON_HEIGHT = 32
    }

    override fun forEachChild(consumer: Consumer<ClickableWidget>) = inner.forEachChild(consumer)

    override fun refreshPositions() = inner.refreshPositions()

    override fun getNavigationFocus(): ScreenRect = inner.navigationFocus

    override fun setPosition(x: Int, y: Int) = inner.setPosition(x, y)

    override fun render(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        if (loading) {
            context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                LOADING_ICON,
                x + (width - ICON_WIDTH) / 2,
                y + (height - ICON_HEIGHT) / 2,
                ICON_WIDTH,
                ICON_HEIGHT,
            )
        }
    }
}