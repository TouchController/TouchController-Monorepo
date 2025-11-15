package top.fifthlight.armorstand.ui.component

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class PlayButton(
    x: Int = 0,
    y: Int = 0,
    width: Int = 20,
    height: Int = 20,
    playing: Boolean = false,
    onPress: PressAction,
) : ButtonWidget(
    x,
    y,
    width,
    height,
    if (playing) {
        PAUSE_TEXT
    } else {
        PLAY_TEXT
    },
    onPress,
    DEFAULT_NARRATION_SUPPLIER,
) {
    companion object {
        private val PAUSE_TEXT = Text.translatable("armorstand.animation.pause")
        private val PLAY_TEXT = Text.translatable("armorstand.animation.play")
        private val PAUSE_ICON = Identifier.of("armorstand", "pause")
        private val PLAY_ICON = Identifier.of("armorstand", "play")
        private const val ICON_WIDTH = 8
        private const val ICON_HEIGHT = 8
    }

    var playing = playing
        set(value) {
            field = value
            message = if (value) {
                PAUSE_TEXT
            } else {
                PLAY_TEXT
            }
        }

    override fun drawMessage(context: DrawContext, textRenderer: TextRenderer, color: Int) {
        val icon = if (playing) {
            PAUSE_ICON
        } else {
            PLAY_ICON
        }
        context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            icon,
            x + (width - ICON_WIDTH) / 2,
            y + (height - ICON_HEIGHT) / 2,
            ICON_WIDTH,
            ICON_HEIGHT,
        )
    }
}