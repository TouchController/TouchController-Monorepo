package top.fifthlight.armorstand.ui.component

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.CheckboxWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

class CheckBoxButton(
    var checked: Boolean,
    private val onClicked: () -> Unit,
) : ClickableWidget(0, 0, SIZE, SIZE, Text.empty()) {
    companion object {
        private const val SIZE = 17
    }

    override fun renderWidget(
        context: DrawContext,
        mouseX: Int,
        mouseY: Int,
        deltaTicks: Float,
    ) {
        val identifier = if (checked) {
            if (this.isFocused) {
                CheckboxWidget.SELECTED_HIGHLIGHTED_TEXTURE
            } else {
                CheckboxWidget.SELECTED_TEXTURE
            }
        } else {
            if (this.isFocused) {
                CheckboxWidget.HIGHLIGHTED_TEXTURE
            } else {
                CheckboxWidget.TEXTURE
            }
        }

        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            identifier,
            x,
            y,
            SIZE,
            SIZE,
        )
    }

    override fun onClick(mouseX: Double, mouseY: Double) = onClicked()

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        builder.put(NarrationPart.TITLE, narrationMessage)
        if (active) {
            if (isFocused) {
                builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.focused"))
            } else {
                builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.hovered"))
            }
        }
    }
}